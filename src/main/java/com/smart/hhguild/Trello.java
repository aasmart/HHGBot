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

package com.smart.hhguild;

import com.julienvey.trello.domain.Board;
import com.julienvey.trello.domain.Card;
import com.julienvey.trello.domain.TList;
import com.julienvey.trello.impl.TrelloImpl;
import com.julienvey.trello.impl.http.JDKTrelloHttpClient;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Trello {
    private static final String trelloCredsFile = "/trellocreds.txt";

    public static final String boardId = "5f89d1cac48ac832ac733bd8";
    public static final String bugListId = "5fa01d2ff2e732803a5ccf38";
    public static final String suggestionsListId = "5fa01d28d8beda0c6b6ef3fd";

    public enum CardType {
        SUGGESTION,
        BUG_FIX
    }

    /**
     * Get's the board from the given ID
     * @param boardId The ID of the board. (Can be found by adding .json to the end of the trello board address and grabbing the first id)
     * @return The found board
     */
    public static Board getBoard(String boardId) {
        try {
            Scanner in = new Scanner(new File(Trello.class.getResource(trelloCredsFile).getFile()));
            TrelloImpl trelloApi = new TrelloImpl(in.nextLine(), in.nextLine(), new JDKTrelloHttpClient());
            return trelloApi.getBoard(boardId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to find Trello credentials file");
            return null;
        }
    }

    /**
     * Gets a list from the specified board
     * @param b The Board
     * @param listId The ID of the list
     * @return The found list object
     */
    public static TList getList(Board b, String listId) {
        List<TList> lists = b.fetchLists();
        return lists.get(lists.stream().map(TList::getId).collect(Collectors.toList()).indexOf(listId));
    }

    public static void createCard(String title, String user, CardType type) {
        Board b = getBoard(boardId);
        if(b == null) {
            System.out.println("Failed to create card (b null)");
            return;
        }

        TList list;
        Card c = new Card();
        if(type == CardType.BUG_FIX) {
            list = getList(b, bugListId);

            c.setDesc("Bug Report by " + user);
            c.setName(title);
        } else if(type == CardType.SUGGESTION) {
            list = getList(b, suggestionsListId);

            c.setDesc("Suggestion by " + user);
            c.setName(title);
        } else {
            return;
        }

        list.createCard(c);
    }
}
