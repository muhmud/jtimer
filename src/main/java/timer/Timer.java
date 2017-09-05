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
import timer.exception.TimerAlreadyRunningException;
import timer.exception.TimerNotRunningException;
import timer.lib.Dates;

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

	public void start(String task, boolean resume) throws NoTaskSpecifiedException, BadLogFileException,
			BadStatusFileException, TimerAlreadyRunningException {
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

		final Date start = new Date();
		final Date anchor;
		final Long workDone;
		if (!resume) {
			anchor = Dates.toDate(start);
			workDone = 0l;
		} else {
			anchor = latestTimerLog.getAnchor();

			// Update the total amount of work done so far
			final TimerStatus status = status();
			if (status != null) {
				workDone = status.getWorkDone()
						+ (latestTimerLog.getEnd().getTime() - latestTimerLog.getStart().getTime());
			} else {
				workDone = 0l;
			}
		}

		write(new TimerLog(anchor, start, null, task));
		writeStatus(TimerStatus.Status.RUNNING, anchor, workDone, start, null);
	}

	public void pause() throws BadLogFileException, BadStatusFileException, TimerNotRunningException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null || latestTimerLog.getEnd() != null) {
			throw new TimerNotRunningException();
		}

		final Date end = new Date();
		write(new TimerLog(latestTimerLog.getAnchor(), latestTimerLog.getStart(), end, latestTimerLog.getTask()));

		writeStatus(TimerStatus.Status.PAUSED, latestTimerLog.getAnchor(), status().getWorkDone(),
				latestTimerLog.getStart(), end);
	}

	public void stop() throws BadLogFileException, BadStatusFileException, TimerNotRunningException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null) {
			throw new BadLogFileException();
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

	public void resume() throws BadLogFileException, TimerAlreadyRunningException, NoTaskSpecifiedException,
			BadStatusFileException {
		// Find the latest timer log entry
		final TimerLog latestTimerLog = latestTimerLog();
		if (latestTimerLog == null) {
			throw new BadLogFileException();
		}

		if (latestTimerLog.getEnd() == null) {
			throw new TimerAlreadyRunningException();
		}

		final String task = latestTimerLog.getTask();
		start(task, true);
	}

	public String check() throws BadStatusFileException {
		// Find the latest timer log entry
		final TimerStatus status = status();
		if (status == null) {
			return "FRESH";
		}

		return status.getStatus().name();
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

							final Date date = Dates.toDate(timerLog.getAnchor());

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
							final Date date = Dates.toDate(timerLog.getAnchor());
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
