package ui

import (
	"context"
	"fmt"
	"math/rand"
	"regexp"
	"strconv"
	"strings"
	"time"

	apiModel "github.com/apache/plc4x/plc4go/pkg/api/model"
	"github.com/apache/plc4x/plc4go/spi"
	"github.com/charmbracelet/bubbles/textinput"
	"github.com/charmbracelet/bubbles/viewport"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
)

type fileEntry struct {
	Name string
	Path string
}

type messageEntry struct {
	Number    int
	Timestamp time.Time
	Origin    string
}

type filesUpdatedMsg struct {
	Files []fileEntry
}

type driversUpdatedMsg struct {
	Drivers []string
}

type messageArrivedMsg struct {
	Entry messageEntry
}

type commandExecResultMsg struct {
	Command  string
	Err      error
	cancelID uint32
}

var commandHistoryShortcut = regexp.MustCompile("^[0-9]$")

var (
	textColor        = lipgloss.Color("15")
	accentColor      = lipgloss.Color("12")
	highlightColor   = lipgloss.Color("10")
	placeholderColor = lipgloss.Color("242")

	titleStyle        = lipgloss.NewStyle().Bold(true).Foreground(textColor).Align(lipgloss.Center)
	subtitleStyle     = lipgloss.NewStyle().Foreground(textColor).Align(lipgloss.Center)
	sectionTitleStyle = lipgloss.NewStyle().Bold(true).Foreground(accentColor)
	sectionBodyStyle  = lipgloss.NewStyle().MarginLeft(2)
	suggestionStyle   = lipgloss.NewStyle().MarginLeft(2).Foreground(highlightColor)
	columnTitleStyle  = lipgloss.NewStyle().Bold(true).Align(lipgloss.Center).Foreground(textColor)
	panelStyle        = lipgloss.NewStyle().Border(lipgloss.NormalBorder()).BorderForeground(textColor).Padding(0, 1)
	placeholderStyle  = lipgloss.NewStyle().MarginLeft(2).Foreground(placeholderColor).Italic(true)
	historyIndexStyle = lipgloss.NewStyle().Foreground(highlightColor).Bold(true).MarginLeft(2).Width(2).Align(lipgloss.Right)
	historyGapStyle   = lipgloss.NewStyle().Foreground(textColor)
	historyTextStyle  = lipgloss.NewStyle().Foreground(textColor)
)

type model struct {
	width  int
	height int

	commandLog logBuffer
	consoleLog logBuffer
	messageLog logBuffer

	files    []fileEntry
	drivers  []string
	messages []messageEntry

	commandInput textinput.Model
	history      []string
	historyIndex int

	suggestions     []string
	suggestionIndex int

	commandViewport viewport.Model
	payloadViewport viewport.Model
	consoleViewport viewport.Model
}

func newModel() *model {
	input := textinput.New()
	input.Prompt = "$ "
	input.Placeholder = ""
	input.CharLimit = 0
	input.Focus()

	history := append([]string(nil), config.History.Last10Commands...)

	cmd := viewport.New(0, 0)
	cmd.Style = lipgloss.NewStyle()

	payload := viewport.New(0, 0)
	payload.Style = lipgloss.NewStyle()

	console := viewport.New(0, 0)
	console.Style = lipgloss.NewStyle()

	mdl := &model{
		commandLog:      newLogBuffer(config.MaxOutputLines),
		consoleLog:      newLogBuffer(config.MaxConsoleLines),
		messageLog:      newLogBuffer(config.MaxOutputLines),
		files:           snapshotFiles(),
		drivers:         snapshotDrivers(),
		commandInput:    input,
		history:         history,
		historyIndex:    len(history),
		suggestions:     nil,
		suggestionIndex: -1,
		commandViewport: cmd,
		payloadViewport: payload,
		consoleViewport: console,
	}

	mdl.updateCommandViewportContent()
	mdl.updatePayloadViewportContent()
	mdl.updateConsoleViewportContent()

	return mdl
}

func SetupProgram() *tea.Program {
	mdl := newModel()
	program := tea.NewProgram(mdl, tea.WithAltScreen(), tea.WithMouseCellMotion())
	dispatcher.setProgram(program)

	commandOutput = newWriter(targetCommand)
	commandOutputClear = makeClearFunc(targetCommand)
	consoleOutput = newWriter(targetConsole)
	consoleOutputClear = makeClearFunc(targetConsole)
	messageOutput = newWriter(targetMessage)
	messageOutputClear = makeClearFunc(targetMessage)

	loadedPcapFilesChanged = func() {
		dispatcher.send(filesUpdatedMsg{Files: snapshotFiles()})
	}
	driverAdded = func(driver string) {
		dispatcher.send(driversUpdatedMsg{Drivers: snapshotDrivers()})
	}
	messageReceived = func(messageNumber int, receiveTime time.Time, message apiModel.PlcMessage) {
		dispatcher.send(messageArrivedMsg{Entry: messageEntry{Number: messageNumber, Timestamp: receiveTime, Origin: "api"}})
		fmt.Fprintf(messageOutput, "API message #%d (%s)\n%v\n", messageNumber, receiveTime.Format("15:04:05.999999"), message)
	}
	spiMessageReceived = func(messageNumber int, receiveTime time.Time, message spi.Message) {
		dispatcher.send(messageArrivedMsg{Entry: messageEntry{Number: messageNumber, Timestamp: receiveTime, Origin: "spi"}})
		fmt.Fprintf(messageOutput, "SPI message #%d (%s)\n%v\n", messageNumber, receiveTime.Format("15:04:05.999999"), message)
	}

	dispatcher.send(filesUpdatedMsg{Files: snapshotFiles()})
	dispatcher.send(driversUpdatedMsg{Drivers: snapshotDrivers()})

	return program
}

func (m *model) Init() tea.Cmd {
	return nil
}

func (m *model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height
		m.updateLayout()
	case tea.KeyMsg:
		return m.handleKey(msg)
	case tea.MouseMsg:
		var cmds []tea.Cmd
		var cmd tea.Cmd
		m.commandViewport, cmd = m.commandViewport.Update(msg)
		if cmd != nil {
			cmds = append(cmds, cmd)
		}
		m.payloadViewport, cmd = m.payloadViewport.Update(msg)
		if cmd != nil {
			cmds = append(cmds, cmd)
		}
		m.consoleViewport, cmd = m.consoleViewport.Update(msg)
		if cmd != nil {
			cmds = append(cmds, cmd)
		}
		return m, tea.Batch(cmds...)
	case appendOutputMsg:
		m.handleAppendOutput(msg)
	case clearOutputMsg:
		m.handleClearOutput(msg)
	case filesUpdatedMsg:
		m.files = append([]fileEntry(nil), msg.Files...)
	case driversUpdatedMsg:
		m.drivers = append([]string(nil), msg.Drivers...)
	case messageArrivedMsg:
		m.messages = append(m.messages, msg.Entry)
		if len(m.messages) > 200 {
			m.messages = append([]messageEntry(nil), m.messages[len(m.messages)-200:]...)
		}
	case commandExecResultMsg:
		delete(cancelFunctions, msg.cancelID)
		if msg.Err != nil {
			fmt.Fprintf(commandOutput, "%s %v\n", time.Now().Format("15:04"), msg.Err)
			return m, nil
		}
		m.commandInput.SetValue("")
		m.commandInput.CursorEnd()
		m.history = append(m.history, msg.Command)
		if len(m.history) > 100 {
			m.history = append([]string(nil), m.history[len(m.history)-100:]...)
		}
		m.historyIndex = len(m.history)
		m.updateCommandViewportContent()
		m.updateLayout()
	}
	return m, nil
}

func (m *model) View() string {
	width := m.width
	if width <= 0 {
		width = 120
	}
	header := titleStyle.Width(width).PaddingBottom(1).Render("PLC4X PCAP Analyzer")
	footer := subtitleStyle.Width(width).PaddingTop(1).Render("https://github.com/apache/plc4x")
	availableHeight := m.height - lipgloss.Height(header) - lipgloss.Height(footer)
	if availableHeight < 8 {
		availableHeight = m.height / 2
		if availableHeight < 8 {
			availableHeight = 8
		}
	}

	const gapWidth = 1
	leftWidth, centerWidth, rightWidth := computeColumnWidths(width, gapWidth)

	leftPanel := renderPanel("", []panelSection{
		{title: "Files", body: renderFiles(m.files)},
		{title: "Registered drivers", body: renderDriverList(m.drivers)},
	}, max(leftWidth-2, 0), availableHeight)

	centerPanel := m.renderOutputPanel(max(centerWidth-2, 0), availableHeight)

	rightPanel := m.renderCommandPanel(max(rightWidth-2, 0), availableHeight)

	gap := lipgloss.NewStyle().Width(gapWidth).Render("")
	columns := lipgloss.JoinHorizontal(lipgloss.Top, leftPanel, gap, centerPanel, gap, rightPanel)
	columns = lipgloss.Place(width, availableHeight, lipgloss.Center, lipgloss.Top, columns)
	content := lipgloss.JoinVertical(lipgloss.Left, header, columns, footer)
	if m.height > 0 {
		content = lipgloss.Place(width, m.height, lipgloss.Center, lipgloss.Top, content)
	}
	return content
}

func (m *model) handleKey(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.Type {
	case tea.KeyCtrlC, tea.KeyCtrlD:
		return m, tea.Quit
	case tea.KeyEnter:
		return m.submitCommand()
	case tea.KeyTab:
		return m.completeCommand(false)
	case tea.KeyShiftTab:
		return m.completeCommand(true)
	case tea.KeyPgUp:
		m.commandViewport.PageUp()
		return m, nil
	case tea.KeyPgDown:
		m.commandViewport.PageDown()
		return m, nil
	case tea.KeyHome:
		m.commandViewport.GotoTop()
		return m, nil
	case tea.KeyEnd:
		m.commandViewport.GotoBottom()
		return m, nil
	case tea.KeyUp:
		if msg.Alt {
			m.commandViewport.ScrollUp(1)
			return m, nil
		}
		m.usePreviousHistory()
		return m, nil
	case tea.KeyDown:
		if msg.Alt {
			m.commandViewport.ScrollDown(1)
			return m, nil
		}
		m.useNextHistory()
		return m, nil
	default:
		oldValue := m.commandInput.Value()
		var cmd tea.Cmd
		m.commandInput, cmd = m.commandInput.Update(msg)
		if m.commandInput.Value() != oldValue {
			m.suggestions = nil
			m.suggestionIndex = -1
			m.updateLayout()
		}
		return m, cmd
	}
}

func (m *model) submitCommand() (tea.Model, tea.Cmd) {
	commandText := strings.TrimSpace(m.commandInput.Value())
	if commandText == "" {
		return m, nil
	}
	if commandText == "quit" {
		return m, tea.Quit
	}
	commandsExecuted++
	resolved, err := resolveHistoryShortcut(commandText)
	if err != nil {
		fmt.Fprintf(commandOutput, "%s %v\n", time.Now().Format("15:04"), err)
		return m, nil
	}
	fmt.Fprintf(commandOutput, "%s %s\n", time.Now().Format("15:04"), resolved)
	m.suggestions = nil
	m.suggestionIndex = -1
	m.updateLayout()
	ctx, cancelFunc := context.WithCancel(rootContext)
	cancelID := rand.Uint32()
	cancelFunctions[cancelID] = cancelFunc
	return m, executeCommand(resolved, cancelID, ctx, cancelFunc)
}

func (m *model) completeCommand(reverse bool) (tea.Model, tea.Cmd) {
	current := m.commandInput.Value()
	if len(m.suggestions) == 0 {
		m.suggestions = rootCommand.Completions(current)
		m.suggestionIndex = -1
		if len(m.suggestions) == 0 {
			return m, nil
		}
	}
	if reverse {
		m.suggestionIndex--
		if m.suggestionIndex < 0 {
			m.suggestionIndex = len(m.suggestions) - 1
		}
	} else {
		m.suggestionIndex = (m.suggestionIndex + 1) % len(m.suggestions)
	}
	completion := m.suggestions[m.suggestionIndex]
	m.commandInput.SetValue(completion)
	m.commandInput.CursorEnd()
	m.updateLayout()
	return m, nil
}

func (m *model) usePreviousHistory() {
	if len(m.history) == 0 {
		return
	}
	if m.historyIndex == 0 {
		m.commandInput.SetValue(m.history[0])
		m.commandInput.CursorEnd()
		return
	}
	m.historyIndex--
	m.commandInput.SetValue(m.history[m.historyIndex])
	m.commandInput.CursorEnd()
}

func (m *model) useNextHistory() {
	if len(m.history) == 0 {
		return
	}
	if m.historyIndex >= len(m.history)-1 {
		m.historyIndex = len(m.history)
		m.commandInput.SetValue("")
		m.commandInput.CursorEnd()
		return
	}
	m.historyIndex++
	m.commandInput.SetValue(m.history[m.historyIndex])
	m.commandInput.CursorEnd()
}

func (m *model) handleAppendOutput(msg appendOutputMsg) {
	switch msg.target {
	case targetCommand:
		m.commandLog.append(msg.text)
		m.updateCommandViewportContent()
	case targetConsole:
		m.consoleLog.append(msg.text)
		m.updateConsoleViewportContent()
	case targetMessage:
		m.messageLog.append(msg.text)
		m.updatePayloadViewportContent()
	}
}

func (m *model) handleClearOutput(msg clearOutputMsg) {
	switch msg.target {
	case targetCommand:
		m.commandLog.clear()
		m.updateCommandViewportContent()
	case targetConsole:
		m.consoleLog.clear()
		m.updateConsoleViewportContent()
	case targetMessage:
		m.messageLog.clear()
		m.updatePayloadViewportContent()
	}
}

func (m *model) renderCommandPanel(width, height int) string {
	panel := panelStyle
	if width > 0 {
		panel = panel.Width(width)
	}
	if height > 0 {
		panel = panel.Height(height)
	}

	innerWidth := width
	if width > 0 {
		frameWidth, _ := panelStyle.GetFrameSize()
		innerWidth = max(width-frameWidth, 0)
	}

	parts := []string{
		columnTitleStyle.Width(innerWidth).Render("Commands"),
		m.commandViewport.View(),
		"",
		sectionTitleStyle.Render("Command Input"),
		sectionBodyStyle.Render(m.commandInput.View()),
	}
	if len(m.suggestions) > 0 {
		suggestions := suggestionStyle.Render(strings.Join(m.suggestions, "    "))
		parts = append(parts, "", sectionTitleStyle.Render("Suggestions"), sectionBodyStyle.Render(suggestions))
	}

	body := lipgloss.JoinVertical(lipgloss.Left, parts...)
	return panel.Render(body)
}

func (m *model) renderOutputPanel(width, height int) string {
	panel := panelStyle
	if width > 0 {
		panel = panel.Width(width)
	}
	if height > 0 {
		panel = panel.Height(height)
	}

	innerWidth := width
	if width > 0 {
		frameWidth, _ := panelStyle.GetFrameSize()
		innerWidth = max(width-frameWidth, 0)
	}

	parts := []string{
		columnTitleStyle.Width(innerWidth).Render("Output"),
		m.payloadViewport.View(),
		"",
		sectionTitleStyle.Render("Console"),
		m.consoleViewport.View(),
	}

	body := lipgloss.JoinVertical(lipgloss.Left, parts...)
	return panel.Render(body)
}

func (m *model) updateLayout() {
	if m.width == 0 && m.height == 0 {
		return
	}

	width := m.width
	if width <= 0 {
		width = 120
	}
	header := titleStyle.Width(width).Render("PLC4X PCAP Analyzer")
	footer := subtitleStyle.Width(width).Render("https://github.com/apache/plc4x")
	availableHeight := m.height - lipgloss.Height(header) - lipgloss.Height(footer)
	if availableHeight < 8 {
		availableHeight = m.height / 2
		if availableHeight < 8 {
			availableHeight = 8
		}
	}

	const gapWidth = 1
	_, centerWidth, rightWidth := computeColumnWidths(width, gapWidth)
	panelFrameWidth, panelFrameHeight := panelStyle.GetFrameSize()
	bodyFrameWidth, _ := sectionBodyStyle.GetFrameSize()

	commandPanelWidth := max(rightWidth-2, 0)
	commandInnerWidth := commandPanelWidth
	if commandPanelWidth > 0 {
		commandInnerWidth = max(commandPanelWidth-panelFrameWidth, 0)
	}
	commandInnerHeight := availableHeight
	if availableHeight > 0 {
		commandInnerHeight = max(availableHeight-panelFrameHeight, 0)
	}

	suggestionsBlock := ""
	if len(m.suggestions) > 0 {
		suggestionsBlock = suggestionStyle.Render(strings.Join(m.suggestions, "    "))
	}

	cmdHeader := columnTitleStyle.Width(commandInnerWidth).Render("Commands")
	inputTitle := sectionTitleStyle.Render("Command Input")
	inputBody := sectionBodyStyle.Render(m.commandInput.View())

	commandStaticHeight := lipgloss.Height(cmdHeader) + lipgloss.Height(inputTitle) + lipgloss.Height(inputBody) + 1
	if suggestionsBlock != "" {
		suggestionsTitle := sectionTitleStyle.Render("Suggestions")
		suggestionsBody := sectionBodyStyle.Render(suggestionsBlock)
		commandStaticHeight += 1 + lipgloss.Height(suggestionsTitle) + lipgloss.Height(suggestionsBody)
	}

	commandViewportHeight := commandInnerHeight - commandStaticHeight
	if commandViewportHeight < 0 {
		commandViewportHeight = 0
	}

	commandViewportWidth := commandInnerWidth - bodyFrameWidth
	if commandViewportWidth < 0 {
		commandViewportWidth = 0
	}

	prevScrollTop := m.commandViewport.YOffset
	m.commandViewport.Width = commandViewportWidth
	m.commandViewport.Height = commandViewportHeight
	m.updateCommandViewportContent()
	if prevScrollTop > 0 {
		m.commandViewport.SetYOffset(prevScrollTop)
	}

	outputPanelWidth := max(centerWidth-2, 0)
	outputInnerWidth := outputPanelWidth
	if outputPanelWidth > 0 {
		outputInnerWidth = max(outputPanelWidth-panelFrameWidth, 0)
	}
	outputInnerHeight := availableHeight
	if availableHeight > 0 {
		outputInnerHeight = max(availableHeight-panelFrameHeight, 0)
	}

	outputHeader := columnTitleStyle.Width(outputInnerWidth).Render("Output")
	consoleTitle := sectionTitleStyle.Render("Console")
	fixedHeight := lipgloss.Height(outputHeader) + 1 + lipgloss.Height(consoleTitle)

	const (
		minPayloadHeight       = 6
		minConsoleHeight       = 4
		preferredConsoleHeight = 10
	)

	availableForViewports := outputInnerHeight - fixedHeight
	if availableForViewports < 0 {
		availableForViewports = 0
	}

	var payloadViewportHeight, consoleViewportHeight int
	if availableForViewports == 0 {
		payloadViewportHeight, consoleViewportHeight = 0, 0
	} else {
		payloadViewportHeight = minPayloadHeight
		consoleViewportHeight = minConsoleHeight
		if payloadViewportHeight+consoleViewportHeight > availableForViewports {
			payloadViewportHeight = min(availableForViewports, minPayloadHeight)
			if payloadViewportHeight < 0 {
				payloadViewportHeight = 0
			}
			consoleViewportHeight = availableForViewports - payloadViewportHeight
			if consoleViewportHeight < 0 {
				consoleViewportHeight = 0
			}
		} else {
			remaining := availableForViewports - (payloadViewportHeight + consoleViewportHeight)
			extraConsoleCapacity := preferredConsoleHeight - minConsoleHeight
			if extraConsoleCapacity > 0 {
				extra := min(extraConsoleCapacity, remaining)
				consoleViewportHeight += extra
				remaining -= extra
			}
			payloadViewportHeight += remaining
		}
	}

	payloadViewportWidth := outputInnerWidth - bodyFrameWidth
	if payloadViewportWidth < 0 {
		payloadViewportWidth = 0
	}
	consoleViewportWidth := outputInnerWidth - bodyFrameWidth
	if consoleViewportWidth < 0 {
		consoleViewportWidth = 0
	}

	m.payloadViewport.Width = payloadViewportWidth
	m.payloadViewport.Height = payloadViewportHeight
	m.consoleViewport.Width = consoleViewportWidth
	m.consoleViewport.Height = consoleViewportHeight

	m.updatePayloadViewportContent()
	m.updateConsoleViewportContent()
}

func resolveHistoryShortcut(commandText string) (string, error) {
	if !commandHistoryShortcut.MatchString(commandText) {
		return commandText, nil
	}
	index, _ := strconv.Atoi(commandHistoryShortcut.FindString(commandText))
	if index >= len(config.History.Last10Commands) {
		return "", fmt.Errorf("no such element %d in command history", index)
	}
	return config.History.Last10Commands[index], nil
}

func executeCommand(command string, cancelID uint32, ctx context.Context, cancel context.CancelFunc) tea.Cmd {
	return func() tea.Msg {
		defer cancel()
		err := Execute(ctx, command)
		return commandExecResultMsg{Command: command, Err: err, cancelID: cancelID}
	}
}

func renderFiles(entries []fileEntry) string {
	if len(entries) == 0 {
		return "(none)"
	}
	rows := make([]string, 0, len(entries))
	for _, entry := range entries {
		rows = append(rows, fmt.Sprintf("%s — %s", entry.Name, entry.Path))
	}
	return strings.Join(rows, "\n")
}

func renderDriverList(entries []string) string {
	if len(entries) == 0 {
		return "(none)"
	}
	return strings.Join(entries, "\n")
}

func renderMessages(entries []messageEntry) string {
	if len(entries) == 0 {
		return "(none)"
	}
	const maxLines = 10
	start := 0
	if len(entries) > maxLines {
		start = len(entries) - maxLines
	}
	rows := make([]string, 0, len(entries)-start)
	for _, entry := range entries[start:] {
		rows = append(rows, fmt.Sprintf("#%d [%s] @ %s", entry.Number, strings.ToUpper(entry.Origin), entry.Timestamp.Format("15:04:05")))
	}
	return strings.Join(rows, "\n")
}

func snapshotFiles() []fileEntry {
	result := make([]fileEntry, 0, len(loadedPcapFiles))
	for _, f := range loadedPcapFiles {
		result = append(result, fileEntry{Name: f.name, Path: f.path})
	}
	return result
}

func snapshotDrivers() []string {
	return append([]string(nil), registeredDriverNames...)
}

func (m *model) updateCommandViewportContent() {
	logBody := strings.TrimSuffix(m.commandLog.String(), "\n")
	logSection := lipgloss.JoinVertical(lipgloss.Left,
		sectionTitleStyle.Render("Command Log"),
		renderBodyBlock(logBody),
	)

	historySection := lipgloss.JoinVertical(lipgloss.Left,
		sectionTitleStyle.Render("Last 10 commands"),
		renderCommandHistoryView(m.history),
	)

	content := lipgloss.JoinVertical(lipgloss.Left, logSection, "", historySection)
	atBottom := m.commandViewport.AtBottom()
	m.commandViewport.SetContent(content)
	if atBottom {
		m.commandViewport.GotoBottom()
	}
}

func (m *model) updatePayloadViewportContent() {
	segments := make([]string, 0, 2)
	messageBody := strings.TrimSuffix(m.messageLog.String(), "\n")
	if strings.TrimSpace(messageBody) != "" {
		segment := lipgloss.JoinVertical(lipgloss.Left,
			sectionTitleStyle.Render("Message Log"),
			renderBodyBlock(messageBody),
		)
		segments = append(segments, segment)
	}
	if len(m.messages) > 0 {
		recent := lipgloss.JoinVertical(lipgloss.Left,
			sectionTitleStyle.Render("Recent Messages"),
			renderBodyBlock(renderMessages(m.messages)),
		)
		segments = append(segments, recent)
	}
	if len(segments) == 0 {
		segments = append(segments, placeholderStyle.Render("(none)"))
	}
	content := strings.Join(segments, "\n\n")
	atBottom := m.payloadViewport.AtBottom()
	m.payloadViewport.SetContent(content)
	if atBottom {
		m.payloadViewport.GotoBottom()
	}
}

func (m *model) updateConsoleViewportContent() {
	consoleBody := strings.TrimSuffix(m.consoleLog.String(), "\n")
	content := renderBodyBlock(consoleBody)
	atBottom := m.consoleViewport.AtBottom()
	m.consoleViewport.SetContent(content)
	if atBottom {
		m.consoleViewport.GotoBottom()
	}
}

func renderCommandHistoryLines(history []string) []string {
	start := len(history) - 10
	if start < 0 {
		start = 0
	}
	rows := make([]string, 0, len(history)-start)
	for idx := start; idx < len(history); idx++ {
		index := historyIndexStyle.Render(fmt.Sprintf("%d", idx))
		divider := historyGapStyle.Render(": ")
		entry := historyTextStyle.Render(history[idx])
		rows = append(rows, lipgloss.JoinHorizontal(lipgloss.Left, index, divider, entry))
	}
	return rows
}

func renderCommandHistoryView(history []string) string {
	if len(history) == 0 {
		return placeholderStyle.Render("(none)")
	}
	rows := renderCommandHistoryLines(history)
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func renderBodyBlock(text string) string {
	if strings.TrimSpace(text) == "" {
		return placeholderStyle.Render("(none)")
	}
	lines := strings.Split(text, "\n")
	styled := make([]string, len(lines))
	for i, line := range lines {
		styled[i] = sectionBodyStyle.Render(line)
	}
	return lipgloss.JoinVertical(lipgloss.Left, styled...)
}

type panelSection struct {
	title string
	body  string
}

func renderPanel(title string, sections []panelSection, width, height int) string {
	var parts []string
	if strings.TrimSpace(title) != "" {
		parts = append(parts, columnTitleStyle.Width(width).Render(title))
	}
	for i, section := range sections {
		if strings.TrimSpace(section.title) != "" {
			parts = append(parts, sectionTitleStyle.Render(section.title))
		}
		body := section.body
		if strings.TrimSpace(body) == "" {
			body = placeholderStyle.Render("(none)")
		} else if !containsANSI(body) {
			body = sectionBodyStyle.Render(body)
		}
		parts = append(parts, body)
		if i != len(sections)-1 {
			parts = append(parts, "")
		}
	}
	if len(sections) == 0 {
		parts = append(parts, placeholderStyle.Render("(none)"))
	}
	inner := strings.Join(parts, "\n")
	style := panelStyle
	if width > 0 {
		style = style.Width(width)
	}
	if height > 0 {
		style = style.Height(height)
	}
	return style.Render(inner)
}

func containsANSI(body string) bool {
	return strings.Contains(body, "\x1b[")
}

func computeColumnWidths(total, gap int) (int, int, int) {
	if total <= 0 {
		total = 120
	}
	gapsWidth := gap * 2
	usable := total - gapsWidth
	if usable < 60 {
		usable = 60
	}
	leftMin, centerMin, rightMin := 24, 32, 24
	left := max(leftMin, usable/4)
	right := max(rightMin, usable/4)
	center := usable - left - right
	if center < centerMin {
		deficit := centerMin - center
		if spare := left - leftMin; spare > 0 {
			shift := min(deficit, spare)
			left -= shift
			deficit -= shift
		}
		if deficit > 0 {
			if spare := right - rightMin; spare > 0 {
				shift := min(deficit, spare)
				right -= shift
				deficit -= shift
			}
		}
		center = usable - left - right
	}
	return left, center, right
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
