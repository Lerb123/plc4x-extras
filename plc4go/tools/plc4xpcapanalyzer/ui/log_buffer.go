package ui

type logBuffer struct {
	lines []string
	max   int
}

func newLogBuffer(max int) logBuffer {
	return logBuffer{lines: []string{""}, max: max}
}

func (b *logBuffer) append(text string) {
	if len(b.lines) == 0 {
		b.lines = []string{""}
	}
	remaining := text
	for len(remaining) > 0 {
		newline := -1
		for i := 0; i < len(remaining); i++ {
			if remaining[i] == '\n' {
				newline = i
				break
			}
		}
		if newline == -1 {
			b.lines[len(b.lines)-1] += remaining
			break
		}
		b.lines[len(b.lines)-1] += remaining[:newline]
		b.lines = append(b.lines, "")
		remaining = remaining[newline+1:]
	}
	b.trim()
}

func (b *logBuffer) clear() {
	b.lines = []string{""}
}

func (b *logBuffer) trim() {
	if b.max <= 0 {
		return
	}
	if len(b.lines) <= b.max {
		return
	}
	offset := len(b.lines) - b.max
	b.lines = append([]string(nil), b.lines[offset:]...)
	if len(b.lines) == 0 {
		b.lines = []string{""}
	}
}

func (b logBuffer) String() string {
	result := ""
	for i, line := range b.lines {
		if i > 0 {
			result += "\n"
		}
		result += line
	}
	return result
}
