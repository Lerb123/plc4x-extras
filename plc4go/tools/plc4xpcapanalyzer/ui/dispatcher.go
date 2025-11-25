package ui

import (
	"sync"

	tea "github.com/charmbracelet/bubbletea"
)

type messageDispatcher struct {
	mu      sync.Mutex
	program *tea.Program
	buffer  []tea.Msg
	queue   chan tea.Msg
}

func newDispatcher() *messageDispatcher {
	d := &messageDispatcher{
		queue: make(chan tea.Msg, 128),
	}
	go d.run()
	return d
}

func (d *messageDispatcher) setProgram(program *tea.Program) {
	d.mu.Lock()
	d.program = program
	buffered := append([]tea.Msg(nil), d.buffer...)
	d.buffer = nil
	d.mu.Unlock()

	go func() {
		for _, msg := range buffered {
			d.queue <- msg
		}
	}()
}

func (d *messageDispatcher) send(msg tea.Msg) {
	d.queue <- msg
}

func (d *messageDispatcher) run() {
	for msg := range d.queue {
		d.mu.Lock()
		program := d.program
		if program == nil {
			d.buffer = append(d.buffer, msg)
			d.mu.Unlock()
			continue
		}
		d.mu.Unlock()

		program.Send(msg)
	}
}
