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

package com.smart.hhguild.Templates.Other;

import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Quests.Quest;
import net.dv8tion.jda.api.entities.Member;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The editor class consists of multiple methods and variables used for an editor object, which is for editing
 * various objects and to have the user stay editing that object. Most notably used with quests.
 */
public class Editor {
    private final Member editor;
    private final EditType editType;
    private EditAction editAction;
    private String editName;
    public ScheduledExecutorService timer;

    /**
     * Edit types is the type of thing the user is editing, such as a quest
     */
    public enum EditType {
        QUEST
    }

    /**
     * Edit actions is what the editor is currently doing, and if nothing, NONE. They are used to run commands in various things,
     * such as quests, depending on their current action
     */
    public enum EditAction {
        NONE,
        QUEST_BASIC_ADD,
        QUEST_FIELD_ADD,
        QUEST_CODE_ADD,
        QUEST_BASIC_REMOVE,
        QUEST_FIELD_REMOVE,
        QUEST_CODE_REMOVE,
        QUEST_BASIC_EDIT,
        QUEST_NAME_EDIT,
        QUEST_METHOD_EDIT,
        QUEST_FIELD_EDIT,
        QUEST_CODE_EDIT,
        QUEST_COOLDOWN_EDIT,
        QUEST_POINT_EDIT,
        QUEST_REMAINING_EDIT,
        QUEST_CLUE_EDIT,
    }

    public Editor(Member editor, EditType editType, String editName) {
        this.editor = editor;
        this.editType = editType;
        this.editName = editName;
        this.editAction = EditAction.NONE;

        updateTimer();
    }

    /**
     * @return The member object of the editor
     */
    public Member getEditor() {
        return editor;
    }

    /**
     * @return The editor's editType
     */
    public EditType getEditType() {
        return editType;
    }

    /**
     * @return A string of the editType (I.e quest) and the editName (I.e quest1) joined by a comma
     */
    public String getEditing() {
        return editType + ":" + editName;
    }

    /**
     * @return Solely the name of the object the editor is editing
     */
    public String getEditName() {
        return editName;
    }

    /**
     * @param editName The name of the object the editor is editing
     */
    public void setEditName(String editName) {
        this.editName = editName;
    }

    /**
     * @param editAction Sets the edit action the user is doing
     */
    public void setEditAction(EditAction editAction) {
        this.editAction = editAction;
    }

    /**
     * @return Returns the editor's current edit action
     */
    public EditAction getEditAction() {
        return editAction;
    }

    /**
     * Resets the timer which stop the user from editing. The default is 5 minutes
     */
    public void updateTimer() {
        try { timer.shutdownNow(); } catch (Exception ignore) {}

        timer = Executors.newScheduledThreadPool(1);
        timer.schedule(() -> {
            Main.editors.remove(this);

            if(editType == EditType.QUEST) {
                try {
                    Quest quest = Quest.readQuest(editName);
                    assert quest != null;
                    quest.clearRelatedMessages(Main.ADMIN_COMMANDS_CHANNEL, quest.getRelatedMessages().size());
                } catch (Exception ignore) {}
            }
            timer.shutdown();
        }, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Editor editor1 = (Editor) o;
        return Objects.equals(editor, editor1.editor) &&
                editType == editor1.editType &&
                Objects.equals(editName, editor1.editName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(editor, editType, editName);
    }
}
