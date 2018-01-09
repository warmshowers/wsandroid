package fi.bitrite.android.ws.persistence.converters;

import fi.bitrite.android.ws.model.Feedback;

public class RelationConverter {
    private static final int CODE_GUEST = 0;
    private static final int CODE_HOST = 1;
    private static final int CODE_MET_WHILE_TRAVELING = 2;
    private static final int CODE_OTHER = 3;

    public static Integer relationToInt(Feedback.Relation relation) {
        if (relation == null) {
            return null;
        }

        switch (relation) {
            case Guest: return CODE_GUEST;
            case Host: return CODE_HOST;
            case MetWhileTraveling: return CODE_MET_WHILE_TRAVELING;
            case Other: return CODE_OTHER;

            default:
                throw new IllegalArgumentException("Invalid relation value: " + relation);
        }
    }

    public static Feedback.Relation intToRelation(Integer relation) {
        if (relation == null) {
            return null;
        }

        switch (relation) {
            case CODE_GUEST: return Feedback.Relation.Guest;
            case CODE_HOST: return Feedback.Relation.Host;
            case CODE_MET_WHILE_TRAVELING: return Feedback.Relation.MetWhileTraveling;
            case CODE_OTHER: return Feedback.Relation.Other;

            default:
                throw new IllegalArgumentException("Invalid relation value: " + relation);
        }
    }
}
