package timer;

import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import timer.exception.BadLogFileException;
import timer.exception.BadStatusFileException;
import timer.exception.NoTaskSpecifiedException;
import timer.exception.TimerAlreadyRunningException;
import timer.exception.TimerNotRunningException;

public final class Main {
	private static final String HOME_DIRECTORY = System.getProperty("user.home");
	private static final String CURRENT_DIRECTORY = Paths.get(".").toAbsolutePath().normalize().toString();

	private static void error(String msg) {
		System.err.println(msg);
		System.exit(1);
	}

	public static void main(String[] args) {
		final TimerCmdLine timerOptions = TimerCmdLine.parse(args);
		if (timerOptions == null) {
			TimerCmdLine.printHelp();
			System.exit(0);
		}

		final Timer timer = new Timer(CURRENT_DIRECTORY, HOME_DIRECTORY);
		try {
			switch (timerOptions.getCommand()) {
			case Go:
				timer.start(timerOptions.getArg());
				break;
			case Stop:
				timer.stop();
				break;
			case Check:
				System.out.println(timer.check());
				break;
			case Summary:
				Date start = null, end = null;
				final String[] commandArgs = timerOptions.getArgs();
				if (commandArgs != null && commandArgs.length > 0) {
					final SimpleDateFormat dateFormat = new SimpleDateFormat(TimerCmdLine.DATE_FORMAT);
					try {
						start = dateFormat.parse(commandArgs[0]);
						if (commandArgs.length > 1) {
							end = dateFormat.parse(commandArgs[1]);
						}
					} catch (ParseException e) {
						error("Start/End dates must be in yyyy-MM-dd format");
					}

					if (start != null && end != null && start.getTime() > end.getTime()) {
						error("Start/End are wrong way round");
					}
				}
				final List<TimerSummary> results = timer.summary(start, end);
				for (TimerSummary result : results) {
					System.out.println(result.format());
				}
				break;
			case Status:
				final TimerStatus timerStatus = timer.status();
				if (timerStatus != null) {
					System.out.println(timerStatus.format());
				}
				break;
			}
		} catch (BadLogFileException e) {
			error(String.format(
					"There's something wrong with the log file. Either delete %s in the current directory or try to fix it",
					Timer.LOG_FILE));
		} catch (BadStatusFileException e) {
			error(String.format("There's something wrong the status file %s in your home directory",
					Timer.STATUS_FILE));
		} catch (TimerAlreadyRunningException e) {
			error("The timer is already running");
		} catch (TimerNotRunningException e) {
			error("The timer is not running");
		} catch (NoTaskSpecifiedException e) {
			error("You need to specify a task");
		}
	}
}
