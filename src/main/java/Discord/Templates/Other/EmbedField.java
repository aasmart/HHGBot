package Discord.Templates.Other;

/**
 * A simple class for adding fields to an embed
 */
public class EmbedField {
    public final String title;
    public final String text;
    public final boolean inline;

    public EmbedField(String title, String text, boolean inline) {
        this.title = title;
        this.text = text;
        this.inline = inline;
    }

    public EmbedField(boolean inline) {
        this.title = "";
        this.text = "";
        this.inline = inline;
    }
}
