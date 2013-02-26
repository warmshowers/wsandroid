package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.model.Feedback;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * User: johannes
 * Date: 25.02.2013
 */
public class FeedbackJsonParserTest {

    @Rule
    public ResourceFile feedbackData = new ResourceFile("feedback.json");

    @Test
    public void testNumberOfItemsIsCorrect() throws IOException {
        FeedbackJsonParser parser = new FeedbackJsonParser(feedbackData.getContent());
        List<Feedback> feedback = parser.getFeedback();
        assertEquals(3, feedback.size());
    }

}
