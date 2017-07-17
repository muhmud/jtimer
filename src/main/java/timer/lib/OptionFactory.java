package timer.lib;

import org.apache.commons.cli.Option;

public final class OptionFactory {
	private OptionFactory() {}

	public static Option create(String opt, String longOpt, int argCount, boolean optional, String description) {
		final Option option = new Option(opt, longOpt, argCount > 0, description);
		if (argCount > 1) {
			option.setArgs(argCount);
			option.setOptionalArg(optional);
		}

		return option;
	}
}
