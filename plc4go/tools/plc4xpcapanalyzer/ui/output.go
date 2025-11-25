package ui

import (
	"io"
	"regexp"
)

type outputTarget int

const (
	targetCommand outputTarget = iota
	targetConsole
	targetMessage
)

type appendOutputMsg struct {
	target outputTarget
	text   string
}

type clearOutputMsg struct {
	target outputTarget
}

type programWriter struct {
	target outputTarget
}

func (w *programWriter) Write(p []byte) (int, error) {
	dispatcher.send(appendOutputMsg{target: w.target, text: string(p)})
	return len(p), nil
}

func newWriter(target outputTarget) io.Writer {
	return &programWriter{target: target}
}

var ansiSequencePattern = regexp.MustCompile(`\x1b\[[0-9;]*[A-Za-z]`)

type ansiStrippingWriter struct {
	target io.Writer
}

func (w *ansiStrippingWriter) Write(p []byte) (int, error) {
	if !ansiSequencePattern.Match(p) {
		return w.target.Write(p)
	}
	clean := ansiSequencePattern.ReplaceAll(p, nil)
	n, err := w.target.Write(clean)
	// Ensure we report having consumed the original input length when possible.
	if err != nil {
		// Best effort to translate number of bytes reported back to the original slice length.
		return len(p) - (len(clean) - n), err
	}
	return len(p), nil
}

func newANSIStrippingWriter(target io.Writer) io.Writer {
	if target == nil {
		return nil
	}
	return &ansiStrippingWriter{target: target}
}

func makeClearFunc(target outputTarget) func() {
	return func() {
		dispatcher.send(clearOutputMsg{target: target})
	}
}
