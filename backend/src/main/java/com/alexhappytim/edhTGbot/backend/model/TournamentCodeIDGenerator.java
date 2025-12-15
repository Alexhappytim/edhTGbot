package com.alexhappytim.edhTGbot.backend.model;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.security.SecureRandom;

public class TournamentCodeIDGenerator implements IdentifierGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        String id;
        do{
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            id = sb.toString();
        }while(existsInDb(session, id));

        return id;
    }
    private boolean existsInDb(SharedSessionContractImplementor session, String id) {
        String sql = "SELECT EXISTS(\n" +
                "    SELECT 1 FROM tournament WHERE id = :id\n" +
                ")\n";
        Object result = session.createNativeQuery(sql)
                .setParameter("id", id)
                .uniqueResult();
        return (boolean) result;
    }
}