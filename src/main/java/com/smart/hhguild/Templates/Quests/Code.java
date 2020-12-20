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
