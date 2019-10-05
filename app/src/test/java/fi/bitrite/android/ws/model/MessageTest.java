package fi.bitrite.android.ws.model;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class MessageTest {

    /**
     * Tests that need Robolectric to run, e.g., because the method tested calls an Android SDK method
     */
    @RunWith(RobolectricTestRunner.class)
    public static class RobolectricMessageTest {
        @Test
        public void cloneForIsNew_sameStateAsOriginal() {
            final Date date = new Date();
            final Message original = new Message(42, 23, 17, date, "Hi!", false, false);
            final Message cloned = original.cloneForIsNew(false);

            assertThat(cloned.isNew, is(equalTo(original.isNew)));

            assertThat(cloned.id, is(equalTo(original.id)));
            assertThat(cloned.threadId, is(equalTo(original.threadId)));
            assertThat(cloned.authorId, is(equalTo(original.authorId)));
            assertThat(cloned.date, is(equalTo(original.date)));
            assertThat(cloned.rawBody, is(equalTo(original.rawBody)));
            assertThat(cloned.isPushed, is(equalTo(original.isPushed)));
        }

        @Test
        public void cloneForIsNew_differentStateThanOriginal() {
            final Message original = new Message(42, 23, 17, new Date(), "Hi!", false, false);
            final Message cloned = original.cloneForIsNew(true);

            assertThat(cloned.isNew, is(not(equalTo(original.isNew))));

            assertThat(cloned.id, is(equalTo(original.id)));
            assertThat(cloned.threadId, is(equalTo(original.threadId)));
            assertThat(cloned.authorId, is(equalTo(original.authorId)));
            assertThat(cloned.date, is(equalTo(original.date)));
            assertThat(cloned.rawBody, is(equalTo(original.rawBody)));
            assertThat(cloned.isPushed, is(equalTo(original.isPushed)));
        }

        @Test
        public void cloneForIsPushed_sameStateAsOriginal() {
            final Date date = new Date();
            final Message original = new Message(42, 23, 17, date, "Hi!", false, false);
            final Message cloned = original.cloneForIsPushed(false);

            assertThat(cloned.isPushed, is(equalTo(original.isPushed)));

            assertThat(cloned.id, is(equalTo(original.id)));
            assertThat(cloned.threadId, is(equalTo(original.threadId)));
            assertThat(cloned.authorId, is(equalTo(original.authorId)));
            assertThat(cloned.date, is(equalTo(original.date)));
            assertThat(cloned.rawBody, is(equalTo(original.rawBody)));
            assertThat(cloned.isNew, is(equalTo(original.isNew)));
        }

        @Test
        public void cloneForIsPushed_differentStateThanOriginal() {
            final Message original = new Message(42, 23, 17, new Date(), "Hi!", false, false);
            final Message cloned = original.cloneForIsPushed(true);

            assertThat(cloned.isPushed, is(not(equalTo(original.isPushed))));

            assertThat(cloned.id, is(equalTo(original.id)));
            assertThat(cloned.threadId, is(equalTo(original.threadId)));
            assertThat(cloned.authorId, is(equalTo(original.authorId)));
            assertThat(cloned.date, is(equalTo(original.date)));
            assertThat(cloned.rawBody, is(equalTo(original.rawBody)));
            assertThat(cloned.isNew, is(equalTo(original.isNew)));
        }

        @Test
        public void parseBody_html() {
            final String testString =
                    "<p>foo<br></p><p>bar<br>baz<br><br></p><p>&nbsp;</p><p>bzz </p>";
            final String expected = "foo\n\nbar\nbaz\n\n\240\n\nbzz ";

            final CharSequence actual = Message.parseBody(testString);

            assertThat(actual.toString(), is(equalTo(expected)));
        }

        @Test
        public void parseBody_newLineCharactersEnd() {
            final String testString =
                    "<p>foo<br></p><p>bar<br>baz<br><br></p><p>&nbsp;</p><p>bzz </p>\n\n";
            final String expected = "foo\n\nbar\nbaz\n\n\240\n\nbzz ";

            final CharSequence actual = Message.parseBody(testString);

            assertThat(actual.toString(), is(equalTo(expected)));
        }

        @Test
        public void parseBody_justNewlineCharacters() {
            final String testString = "<br><br>";
            final String expected = "";

            final CharSequence actual = Message.parseBody(testString);

            assertThat(actual.toString(), is(equalTo(expected)));
        }

        @Test
        public void parseBody_newlineCharactersStart() {
            final String testString = "<br><br>Test";
            final String expected = "\n\nTest";

            final CharSequence actual = Message.parseBody(testString);

            assertThat(actual.toString(), is(equalTo(expected)));
        }

        @Test
        public void parseBody_empty() {
            final String testString = "<p></p>";
            final String expected = "";

            final CharSequence actual = Message.parseBody(testString);
            assertThat(actual.toString(), is(equalTo(expected)));
        }
    }

    /**
     * Tests we don’t need Robolectric for. Makes matters faster to have them here.
     */
    public static class CleanMessageTest {
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
            final String testString =
                    "<p>Hi there!<br>How are you?</p>\r\n<p></p>\r\n<p>Bye</p>\r\n";
            final String expected = "<p>Hi there!<br>How are you?</p><p></p><p>Bye</p>";

            final String actual = Message.stripBody(testString);

            assertThat(actual, is(equalTo(expected)));
        }
    }
}
