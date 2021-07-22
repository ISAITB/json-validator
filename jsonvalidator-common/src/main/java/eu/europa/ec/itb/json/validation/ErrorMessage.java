package eu.europa.ec.itb.json.validation;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * DTO to capture the information of a single error.
 */
public class ErrorMessage {

    private String message;
    private String branch;
    private Integer locationLine;
    private Integer locationColumn;
    private String locationPointer;
    private boolean inSet = false;

    /**
     * Process the provided raised messages and transform them to a list of DTOs.
     *
     * @param errorMessages The error messages.
     * @param branchMessages The messages linked to different validation  branches.
     * @return The error message DTOs to process.
     */
    public static List<ErrorMessage> processMessages(List<String> errorMessages, Set<String> branchMessages) {
        List<ErrorMessage> processedMessages = new ArrayList<>(errorMessages.size());
        Iterator<String> msgIterator = errorMessages.iterator();
        String currentBranch = null;
        while (msgIterator.hasNext()) {
            String msg = msgIterator.next();
            ErrorMessage msgObj;
            if (branchMessages.contains(msg)) {
                msgObj = new ErrorMessage();
                msgObj.setMessage(msg);
                currentBranch = null;
            } else {
                msgObj = processMessage(msg);
                if (msgObj.getBranch() != null) {
                    currentBranch = msgObj.getBranch();
                    msgObj.setMessage("- [Set "+currentBranch+"]: " + msgObj.getMessage());
                } else if (msgObj.isInSet() && currentBranch != null) {
                    msgObj.setMessage("- [Set "+currentBranch+"]: " + msgObj.getMessage());
                } else {
                    currentBranch = null;
                }
            }
            if (msgObj.getLocationLine() == null) {
                msgObj.setLocationLine(0);
            }
            if (msgObj.getLocationColumn() == null) {
                msgObj.setLocationColumn(0);
            }
            processedMessages.add(msgObj);
        }
        return processedMessages;
    }

    /**
     * Extract a DTO representation of a string-based error message.
     *
     * @param errorMessage The error message text.
     * @return The DTO.
     */
    public static ErrorMessage processMessage(String errorMessage) {
        ErrorMessage msgObj = new ErrorMessage();
        /* Messages can be of the form:
           - CASE 1: [LINE,COL][POINTER] MESSAGE.
           - CASE 2: MESSAGE
           - CASE 3: n) [LINE,COL][POINTER] MESSAGE.
           - CASE 4:    [LINE,COL][POINTER] MESSAGE.
         */
        int parenthesisPosition = errorMessage.indexOf(')');
        int openingBracketPosition = errorMessage.indexOf('[');
        if (parenthesisPosition > 0 && openingBracketPosition > parenthesisPosition && errorMessage.length() > parenthesisPosition+1) {
            // CASE 3.
            msgObj.setBranch(errorMessage.substring(0, parenthesisPosition));
            errorMessage = errorMessage.substring(parenthesisPosition+1).trim();
        } else if (openingBracketPosition > 0) {
            if (StringUtils.isBlank(errorMessage.substring(0, openingBracketPosition))) {
                // CASE 4
                msgObj.setInSet(true);
                errorMessage = errorMessage.trim();
            }
        }
        String locationLineColumn = null;
        String message = null;
        if (errorMessage.startsWith("[")) {
            int firstClosingBracketPosition = errorMessage.indexOf(']');
            if (firstClosingBracketPosition > 0) {
                locationLineColumn = errorMessage.substring(1, firstClosingBracketPosition);
                int finalClosingBracketPosition = errorMessage.indexOf(']', firstClosingBracketPosition+1);
                if (finalClosingBracketPosition <= 0) {
                    finalClosingBracketPosition = firstClosingBracketPosition;
                } else {
                    msgObj.setLocationPointer(errorMessage.substring(firstClosingBracketPosition+2, finalClosingBracketPosition));
                }
                message = errorMessage.substring(finalClosingBracketPosition+1).trim();
            }
        }
        String[] locationLineColumnParts = StringUtils.split(locationLineColumn, ',');
        if (locationLineColumnParts != null && locationLineColumnParts.length > 0) {
            try {
                msgObj.setLocationLine(Integer.valueOf(locationLineColumnParts[0]));
            } catch (NumberFormatException nfe) {
                // Do nothing.
            }
            try {
                msgObj.setLocationColumn(Integer.valueOf(locationLineColumnParts[1]));
            } catch (NumberFormatException nfe) {
                // Do nothing.
            }
        }
        if (message == null) {
            message = errorMessage;
        }
        if (StringUtils.isNotBlank(msgObj.getLocationPointer())) {
            message = "["+msgObj.getLocationPointer()+"] " + message;
        }
        msgObj.setMessage(message);
        return msgObj;
    }

    /**
     * @param message The message.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @param branch The validation branch.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @param locationLine The message's line.
     */
    public void setLocationLine(Integer locationLine) {
        this.locationLine = locationLine;
    }

    /**
     * @param locationColumn The message's column.
     */
    public void setLocationColumn(Integer locationColumn) {
        this.locationColumn = locationColumn;
    }

    /**
     * @param locationPointer The JSON pointer string for the message.
     */
    public void setLocationPointer(String locationPointer) {
        this.locationPointer = locationPointer;
    }

    /**
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The validation branch.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @return The message's location line.
     */
    public Integer getLocationLine() {
        return locationLine;
    }

    /**
     * @return The message's location oclumn.
     */
    public Integer getLocationColumn() {
        return locationColumn;
    }

    /**
     * @return The JSON pointer text for the message's location.
     */
    public String getLocationPointer() {
        return locationPointer;
    }

    /**
     * @return True if the error is in a set of branch errors.
     */
    public boolean isInSet() {
        return inSet;
    }

    /**
     * @param inSet True if the error is in a set of branch errors.
     */
    public void setInSet(boolean inSet) {
        this.inSet = inSet;
    }
}
