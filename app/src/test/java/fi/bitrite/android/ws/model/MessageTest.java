package fi.bitrite.android.ws.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class MessageTest {

    @Test
    public void cloneForIsNew_sameStateAsOriginal() {
        final Date date = new Date();
        final Message original = new Message(42, 23, 17, date, "Hi!", false, false);
        final Message cloned = original.cloneForIsNew(false);

        assertThat(cloned.isNew, is(equalTo(original.isNew)));
    }

    @Test
    public void cloneForIsNew_differentStateThanOriginal() {
        final Message original = new Message(42, 23, 17, new Date(), "Hi!", false, false);
        final Message cloned = original.cloneForIsNew(true);

        assertThat(cloned.isNew, is(not(equalTo(original))));
    }

    @Test
    public void cloneForIsPushed_sameStateAsOriginal() {
        final Date date = new Date();
        final Message original = new Message(42, 23, 17, date, "Hi!", false, false);
        final Message cloned = original.cloneForIsPushed(false);

        assertThat(cloned.isNew, is(equalTo(original.isNew)));
    }

    @Test
    public void cloneForIsPushed_differentStateThanOriginal() {
        final Message original = new Message(42, 23, 17, new Date(), "Hi!", false, false);
        final Message cloned = original.cloneForIsPushed(true);

        assertThat(cloned.isNew, is(not(equalTo(original))));
    }

    @Test
    public void stripBody_emailReply_enclosesInParagraphTags() {
        final String testString = "Hi there! How are you?";
        final String expected = "<p>Hi there! How are you?</p>";

        final String actual = Message.stripBody(testString);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void stripBody_emailReply_enclosesInParagraphTagsReplacesNewlines() {
        final String testString = "Hi there!\r\nHow are you?";
        final String expected = "<p>Hi there!<br>How are you?</p>";

        final String actual = Message.stripBody(testString);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void stripBody_normalReply_replacesNewlines() {
        final String testString = "<p>Hi there!<br>How are you?</p>\r\n<p></p>\r\n<p>Bye</p>\r\n";
        final String expected = "<p>Hi there!<br>How are you?</p><p></p><p>Bye</p>";

        final String actual = Message.stripBody(testString);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void parseBody() {
        final String testString = "<p>foo</p><p>bar<br>baz<br></p>";
        final String expected = "foo\n\nbar\nbaz\n";

        final CharSequence actual = Message.parseBody(testString);

        assertThat(actual, is(equalTo(expected)));
    }
}
