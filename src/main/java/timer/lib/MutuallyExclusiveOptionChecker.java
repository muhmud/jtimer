package timer.lib;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public final class MutuallyExclusiveOptionChecker {
	public Option check(CommandLine commandLine, Option... options) {
		final List<Option> selectedOptions = new ArrayList<>();
		for (Option option : options) {
			if (commandLine.hasOption(option.getOpt())) {
				selectedOptions.add(option);
			}
		}

		return selectedOptions.size() == 1 ? selectedOptions.get(0) : null;
	}
}
