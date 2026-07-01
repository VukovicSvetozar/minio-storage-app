package com.minio.storage.util;

public class EmailTemplates {

    public static String getVerificationEmail(String username, String link) {
        return """
                Zdravo %s,
                
                Hvala Vam što ste se registrovali na MinIO Storage platformu!
                
                Molimo Vas da kliknete na link ispod kako biste verifikovali Vašu email adresu:
                %s
                
                Ovaj link će biti validan naredna 24 časa.
                
                Ako Vi niste kreirali ovaj nalog, slobodno ignorišite ovaj email.
                
                Srdačan pozdrav,
                MinIO Storage Tim
                """.formatted(username, link);
    }

    public static String getWelcomeEmail(String username, String loginUrl) {
        return """
                Zdravo %s,
                
                Vaša email adresa je uspješno verifikovana!
                
                Sada možete koristiti sve funkcionalnosti MinIO Storage sistema:
                - Skladištenje i upravljanje datotekama
                - Dijeljenje fajlova sa drugim korisnicima
                - Preuzimanje i organizacija Vašeg prostora
                
                Prijavite se ovdje: %s
                
                Hvala Vam što koristite naš sistem!
                
                Srdačan pozdrav,
                MinIO Storage Tim
                """.formatted(username, loginUrl);
    }

    public static String getPasswordResetEmail(String username, String link) {
        return """
                Zdravo %s,
                
                Primili smo zahtjev za resetovanje Vaše lozinke.
                
                Kliknite na link ispod kako biste postavili novu lozinku:
                %s
                
                Ovaj link ističe za 1 sat.
                
                Ako Vi niste poslali ovaj zahtjev, molimo Vas da ignorišete ovaj email. Vaša trenutna lozinka će ostati nepromijenjena.
                
                Srdačan pozdrav,
                MinIO Storage Tim
                """.formatted(username, link);
    }

}