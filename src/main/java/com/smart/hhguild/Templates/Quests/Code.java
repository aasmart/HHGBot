/*
 * Copyright (c) 2020 aasmart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.smart.hhguild.Templates.Quests;

import com.smart.hhguild.Main;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.Date;

public class Code implements Serializable {
    private String code;
    private int points;
    private int maxSubmits;
    private boolean isImage;
    private Date releaseTime;

    public Code(String code, int points, int maxSubmits, boolean isImage, Date releaseTime) {
        this.code = code;
        this.points = points;
        this.maxSubmits = maxSubmits;
        this.isImage = isImage;
        this.releaseTime = releaseTime;
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

    /**
     * Gets if the code is an image code or not
     * @return True if the code is an image code
     */
    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public boolean isTime() {
        return Main.canRelease(releaseTime);
    }

    @SuppressWarnings("unchecked")
    public void release() {
        JSONObject codes = Main.readJSONObject(Main.VALID_CODES_FILE);
        JSONArray validCodes = (JSONArray) codes.get("codes");

        JSONObject jsonCode = new JSONObject();

        // Create all variables in code
        jsonCode.put("code", getCode());
        jsonCode.put("points", getPoints());
        jsonCode.put("maxsubmits", getMaxSubmits());
        jsonCode.put("submits", 0);
        jsonCode.put("submitters", new JSONArray());
        jsonCode.put("isImage", isImage());

        validCodes.add(jsonCode);
        Main.writeJSONObjectToFile(codes, Main.VALID_CODES_FILE);
    }
}
