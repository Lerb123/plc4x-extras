package ui

import "io"

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

func makeClearFunc(target outputTarget) func() {
	return func() {
		dispatcher.send(clearOutputMsg{target: target})
	}
}
