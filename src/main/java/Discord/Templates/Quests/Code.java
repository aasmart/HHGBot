package Discord.Templates.Quests;

import java.io.Serializable;

public class Code implements Serializable {
    private String code;
    private int points;
    private int maxSubmits;
    private boolean isImage;

    public Code(String code, int points, int maxSubmits, boolean isImage) {
        this.code = code;
        this.points = points;
        this.maxSubmits = maxSubmits;
        this.isImage = isImage;
    }

    /**
     * Method for getting the code's code
     * @return The code's code
     */
    public String getCode() {
        return code;
    }

    /**
     * Method for setting the code's code
     * @param code The code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Method for getting the code's point value
     * @return The code's point value
     */
    public int getPoints() {
        return points;
    }

    /**
     * Method for setting the code's point value
     * @param points The point value
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * Method for getting the code's maximum submissions
     * @return THe code's maximum submissions
     */
    public int getMaxSubmits() {
        return maxSubmits;
    }

    /**
     * Method for setting the code's maximum submissions
     * @param maxSubmits The maximum submissions
     */
    public void setMaxSubmits(int maxSubmits) {
        this.maxSubmits = maxSubmits;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }
}
