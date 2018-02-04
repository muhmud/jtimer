package timer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.input.ReversedLinesFileReader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.exception.BadLogFileException;
import timer.exception.BadStatusFileException;
import timer.exception.NoTaskSpecifiedException;
import timer.exception.OtherTimerException;
import timer.exception.TimerAlreadyRunningException;
import timer.exception.TimerNotRunningException;

@AllArgsConstructor
public final class Timer {
	public static final String LOG_FILE = ".timer";
	public static final String STATUS_FILE = ".timer-status";

	@Getter
	private final String directory;

	@Getter
	private final String statusDirectory;

	private String logFilePath(boolean find) {
		String logFilePath = Paths.get(directory, LOG_FILE).toString();

		if (find) {
			String directory = this.directory;
			while (!new File(logFilePath).exists()) {
				directory = new File(directory).getParent();
				if (directory == null) {
					logFilePath = null;
					break;
				}

				logFilePath = Paths.get(directory, LOG_FILE).toString();
			}
		}

		return logFilePath;
	}

	private String logFilePath() {
		final String logFilePath = logFilePath(true);
		if (logFilePath != null) {
			return logFilePath;
		}

		return logFilePath(false);
	}

	private String statusFilePath() {
		return Paths.get(statusDirectory, STATUS_FILE).toString();
	}

	private String project() {
		final String logFilePath = logFilePath();
		return java.nio.file.Paths.get(new File(logFilePath).getParent()).getFileName().toString();
	}

	private TimerLog latestTimerLog() throws BadLogFileException {
		final String logFilePath = logFilePath(true);
		if (logFilePath != null) {
			String logLine;
			try (final ReversedLinesFileReader reader =
					new ReversedLinesFileReader(new File(logFilePath), StandardCharsets.UTF_8)) {
				do {
					logLine = reader.readLine();
				} while (logLine != null && logLine.length() == 0);
			} catch (IOException e) {
				throw new BadLogFileException();
			}

			return TimerLog.parse(logLine);
		}

		return null;
	}

	private void write(TimerLog timerLog) throws BadLogFileException {
		try (final FileWriter writer = new FileWriter(logFilePath(), true)) {
			TimerLog.write(writer, timerLog);
		} catch (IOException e) {
			throw new BadLogFileException();
		}
	}

	private void writeStatus(TimerStatus.Status status, Date anchor, Long workDone, Date start, Date end)
			throws BadStatusFileException {
		try (final FileWriter writer = new FileWriter(statusFilePath())) {
			TimerStatus.write(writer, status, project(), new File(logFilePath(true)).getParent(), anchor, workDone,
					start, end);
		} catch (IOException e) {
			throw new BadStatusFileException();
		}
	}

	private long calculateWorkDone(Date anchor) throws BadLogFileException {
		long result = 0;
		final File logFile = new File(logFilePath());
		if (logFile.exists()) {
			try (final ReversedLinesFileReader reader =
					new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
				String line;
				while ((line = reader.readLine()) != null) {
					final TimerLog timerLog = TimerLog.parse(line);
					if (timerLog.getAnchor().equals(anchor)) {
						if (timerLog.getStart() == null) {
							throw new BadLogFileException();
						}
						if (timerLog.getEnd() != null) {
							result += timerLog.getEnd().getTime() - timerLog.getStart().getTime();
						}
					} else {
						break;
					}
				}
			} catch (IOException e) {
				throw new BadLogFileException();
			}
		}

		return result;
	}

	public void start(String task, boolean resume) throws NoTaskSpecifiedException, BadLogFileException,
			BadStatusFileException, TimerAlreadyRunningException, OtherTimerException {
		if (task == null || task.trim().length() == 0) {
			throw new NoTaskSpecifiedException();
		}

		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog != null) {
			if (latestTimerLog.getEnd() == null) {
				throw new TimerAlreadyRunningException();
			}
		}

		// Check the status of the timer (maybe another one is running)
		final TimerStatus timerStatus = status();
		if (timerStatus != null) {
			if (!timerStatus.getDirectory().equals(directory)
					&& timerStatus.getStatus() == TimerStatus.Status.PAUSED) {
				throw new OtherTimerException();
			}
		}

		final Date start = new Date();
		final Date anchor;
		if (!resume) {
			anchor = start;
		} else {
			anchor = latestTimerLog.getAnchor();
		}

		final Long workDone = calculateWorkDone(anchor);

		write(new TimerLog(anchor, start, null, task));
		writeStatus(TimerStatus.Status.RUNNING, anchor, workDone, start, null);
	}

	public void pause()
			throws BadLogFileException, BadStatusFileException, TimerNotRunningException, OtherTimerException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null || latestTimerLog.getEnd() != null) {
			throw new TimerNotRunningException();
		}

		final TimerStatus timerStatus = status();
		if (timerStatus != null && !timerStatus.getDirectory().equals(directory)) {
			throw new OtherTimerException();
		}

		final Date end = new Date();
		write(new TimerLog(latestTimerLog.getAnchor(), latestTimerLog.getStart(), end, latestTimerLog.getTask()));

		writeStatus(TimerStatus.Status.PAUSED, latestTimerLog.getAnchor(), status().getWorkDone(),
				latestTimerLog.getStart(), end);
	}

	public void stop() throws BadLogFileException, BadStatusFileException, OtherTimerException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null) {
			throw new BadLogFileException();
		}

		final TimerStatus timerStatus = status();
		if (timerStatus != null && !timerStatus.getDirectory().equals(directory)) {
			throw new OtherTimerException();
		}

		final Date end;
		if (latestTimerLog.getEnd() == null) {
			end = new Date();
			write(new TimerLog(latestTimerLog.getAnchor(), latestTimerLog.getStart(), end,
					latestTimerLog.getTask()));
		} else {
			end = latestTimerLog.getEnd();
		}

		writeStatus(TimerStatus.Status.STOPPED, latestTimerLog.getAnchor(), status().getWorkDone(),
				latestTimerLog.getStart(), end);
	}

	public void resume(String task) throws BadLogFileException, TimerAlreadyRunningException,
			NoTaskSpecifiedException, BadStatusFileException, OtherTimerException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null) {
			throw new BadLogFileException();
		}

		if (latestTimerLog.getEnd() == null) {
			throw new TimerAlreadyRunningException();
		}

		final String newTask = task != null ? task : latestTimerLog.getTask();
		start(newTask, true);
	}

	public TimerStatus.Status check() throws BadStatusFileException {
		// Find the latest timer log entry
		final TimerStatus status = status();
		if (status == null) {
			return TimerStatus.Status.FRESH;
		}

		return status.getStatus();
	}

	public List<TimerDetail> detail(Date start, Date end) throws BadLogFileException {
		final List<TimerDetail> result = new ArrayList<>();
		final String logFilePath = logFilePath();
		if (new File(logFilePath).exists()) {
			final SortedMap<Date, SortedMap<String, Long>> tmpResults = new TreeMap<>();
			try {
				final Iterator<String> logLines = Files.lines(Paths.get(logFilePath)).iterator();
				while (logLines.hasNext()) {
					final TimerLog timerLog = TimerLog.parse(logLines.next());
					if (timerLog != null && timerLog.getStart() != null && timerLog.getEnd() != null) {
						if ((start == null || timerLog.getAnchor().getTime() >= start.getTime())
								&& (end == null || timerLog.getAnchor().getTime() <= end.getTime())) {
							String task = null;
							if (timerLog.getTask() == null || timerLog.getTask().trim().equals("")
									|| timerLog.getTask().toLowerCase().equals("null")) {
								task = "<No Task>";
							} else {
								task = timerLog.getTask();
							}

							final Date date = timerLog.getAnchor();

							SortedMap<String, Long> timeSpent = tmpResults.get(date);
							if (timeSpent == null) {
								timeSpent = new TreeMap<>();
								tmpResults.put(date, timeSpent);
							}

							Long current = timeSpent.get(task);
							if (current == null) {
								current = 0l;
							}

							current += (timerLog.getEnd().getTime() - timerLog.getStart().getTime()) / 1000;
							timeSpent.put(task, current);
						}
					}
				}

				for (Map.Entry<Date, SortedMap<String, Long>> entry : tmpResults.entrySet()) {
					result.add(new TimerDetail(entry.getKey(), entry.getValue()));
				}
			} catch (IOException e) {
				throw new BadLogFileException();
			}
		}
		return result;
	}

	public List<TimerTaskReport> task(Date start, Date end) throws BadLogFileException {
		final List<TimerTaskReport> result = new ArrayList<>();
		final String logFilePath = logFilePath();
		if (new File(logFilePath).exists()) {
			final SortedMap<String, Long> tmpResults = new TreeMap<>();
			try {
				final Iterator<String> logLines = Files.lines(Paths.get(logFilePath)).iterator();
				while (logLines.hasNext()) {
					final TimerLog timerLog = TimerLog.parse(logLines.next());
					if (timerLog != null && timerLog.getStart() != null && timerLog.getEnd() != null) {
						if ((start == null || timerLog.getAnchor().getTime() >= start.getTime())
								&& (end == null || timerLog.getAnchor().getTime() <= end.getTime())) {
							String task = null;
							if (timerLog.getTask() == null || timerLog.getTask().trim().equals("")
									|| timerLog.getTask().toLowerCase().equals("null")) {
								task = "<No Task>";
							} else {
								task = timerLog.getTask();
							}

							Long current = tmpResults.get(task);
							if (current == null) {
								current = 0l;
							}

							current += (timerLog.getEnd().getTime() - timerLog.getStart().getTime()) / 1000;
							tmpResults.put(task, current);
						}
					}
				}

				for (Map.Entry<String, Long> entry : tmpResults.entrySet()) {
					result.add(new TimerTaskReport(entry.getKey(), entry.getValue()));
				}
			} catch (IOException e) {
				throw new BadLogFileException();
			}
		}
		return result;
	}

	public List<TimerSummary> summary(Date start, Date end) throws BadLogFileException {
		final List<TimerSummary> result = new ArrayList<>();
		final String logFilePath = logFilePath();
		if (new File(logFilePath).exists()) {
			final SortedMap<Date, Long> tmpResults = new TreeMap<>();
			try {
				final Iterator<String> logLines = Files.lines(Paths.get(logFilePath)).iterator();
				while (logLines.hasNext()) {
					final TimerLog timerLog = TimerLog.parse(logLines.next());
					if (timerLog != null && timerLog.getStart() != null && timerLog.getEnd() != null) {
						if ((start == null || timerLog.getAnchor().getTime() >= start.getTime())
								&& (end == null || timerLog.getAnchor().getTime() <= end.getTime())) {
							final Date date = timerLog.getAnchor();
							Long current = tmpResults.get(date);
							if (current == null) {
								current = 0l;
							}
							current += (timerLog.getEnd().getTime() - timerLog.getStart().getTime()) / 1000;
							tmpResults.put(date, current);
						}
					}
				}

				for (Map.Entry<Date, Long> entry : tmpResults.entrySet()) {
					result.add(new TimerSummary(entry.getKey(), entry.getValue()));
				}
			} catch (IOException e) {
				throw new BadLogFileException();
			}
		}

		return result;
	}

	public TimerStatus status() throws BadStatusFileException {
		final File statusFile = new File(statusFilePath());
		if (statusFile.exists()) {
			String statusLine;
			try (final BufferedReader reader = new BufferedReader(new FileReader(statusFile))) {
				statusLine = reader.readLine();
			} catch (IOException e) {
				throw new BadStatusFileException();
			}

			return TimerStatus.parse(statusLine);
		}

		return null;
	}
}
