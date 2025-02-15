package com.tyron.builder.internal.logging.console;

import com.google.common.collect.Lists;
import com.tyron.builder.internal.logging.text.StyledTextOutput;
import com.tyron.builder.internal.logging.events.StyledTextOutputEvent;
import com.tyron.builder.internal.logging.format.TersePrettyDurationFormatter;
import com.tyron.builder.internal.nativeintegration.console.ConsoleMetaData;

import java.util.List;

public class ProgressBar {
    private final TersePrettyDurationFormatter elapsedTimeFormatter = new TersePrettyDurationFormatter();

    private final ConsoleMetaData consoleMetaData;
    private final String progressBarPrefix;
    private final int progressBarWidth;
    private final String progressBarSuffix;
    private final char fillerChar;
    private final char incompleteChar;
    private final String suffix;

    private int current;
    private int total;
    private boolean failing;
    private String lastElapsedTimeStr;
    private List<StyledTextOutputEvent.Span> formatted;

    public ProgressBar(ConsoleMetaData consoleMetaData, String progressBarPrefix, int progressBarWidth, String progressBarSuffix, char completeChar, char incompleteChar, String suffix, int initialProgress, int totalProgress) {
        this.consoleMetaData = consoleMetaData;
        this.progressBarPrefix = progressBarPrefix;
        this.progressBarWidth = progressBarWidth;
        this.progressBarSuffix = progressBarSuffix;
        this.fillerChar = completeChar;
        this.incompleteChar = incompleteChar;
        this.suffix = suffix;
        this.current = initialProgress;
        this.total = totalProgress;
    }

    public void moreProgress(int totalProgress) {
        total += totalProgress;
        formatted = null;
    }

    public void update(boolean failing) {
        this.current++;
        this.failing = this.failing || failing;
        formatted = null;
    }

    public List<StyledTextOutputEvent.Span> formatProgress(boolean timerEnabled, long elapsedTime) {
        String elapsedTimeStr = elapsedTimeFormatter.format(elapsedTime);
        if (formatted == null || !elapsedTimeStr.equals(lastElapsedTimeStr)) {
            int consoleCols = consoleMetaData.getCols();
            int completedWidth = (int) ((current * 1.0) / total * progressBarWidth);
            int remainingWidth = progressBarWidth - completedWidth;

            String statusPrefix = trimToConsole(consoleCols, 0, progressBarPrefix);
            String coloredProgress = trimToConsole(consoleCols, statusPrefix.length(), fill(fillerChar, completedWidth));
            String statusSuffix = trimToConsole(consoleCols, coloredProgress.length(), fill(incompleteChar, remainingWidth)
                                                                                       + progressBarSuffix + " " + (int) (current * 100.0 / total) + '%' + ' ' + suffix
                                                                                       + (timerEnabled ? " [" + elapsedTimeStr + "]" : ""));

            lastElapsedTimeStr = elapsedTimeStr;
            formatted = Lists.newArrayList(
                    new StyledTextOutputEvent.Span(StyledTextOutput.Style.Header, statusPrefix),
                    new StyledTextOutputEvent.Span(failing ? StyledTextOutput.Style.FailureHeader : StyledTextOutput.Style.SuccessHeader, coloredProgress),
                    new StyledTextOutputEvent.Span(StyledTextOutput.Style.Header, statusSuffix));
        }
        return formatted;
    }

    private String fill(char ch, int count) {
        char[] chars = new char[count];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ch;
        }
        return new String(chars);
    }

    private String trimToConsole(int cols, int prefixLength, String str) {
        int consoleWidth = cols - 1;
        int remainingWidth = consoleWidth - prefixLength;

        if (consoleWidth < 0) {
            return str;
        }
        if (remainingWidth <= 0) {
            return "";
        }
        if (consoleWidth < str.length()) {
            return str.substring(0, consoleWidth);
        }
        return str;
    }
}

