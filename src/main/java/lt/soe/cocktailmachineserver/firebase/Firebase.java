package lt.soe.cocktailmachineserver.firebase;

import com.google.firebase.database.*;
import lt.soe.cocktailmachineserver.ServerResponse;
import lt.soe.cocktailmachineserver.cocktail.Cocktail;
import lt.soe.cocktailmachineserver.pumps.PumpsConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Firebase {

    public List<Cocktail> getCocktails() {
        final Semaphore semaphore = new Semaphore(0);
        List<Cocktail> cocktails = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/");
        System.out.println("waiting for cocktails from firebase...");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    Cocktail cocktail = snap.getValue(Cocktail.class);
                    cocktails.add(cocktail);
                }
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("get cocktails from firebase failed");
                error.toException().printStackTrace();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return cocktails;
    }

    public ServerResponse constructCocktail(int index, Cocktail cocktail) {
        Semaphore semaphore = new Semaphore(0);
        ServerResponse serverResponse = new ServerResponse();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/" + index);
        ref.setValue(cocktail, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                serverResponse.successful = true;
                serverResponse.message = "added cocktail " + cocktail.name + " to firebase successfully";
                System.out.println(serverResponse.message);
                semaphore.release();

            } else {
                serverResponse.successful = true;
                serverResponse.message = databaseError.getMessage();
                databaseError.toException().printStackTrace();
                semaphore.release();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return serverResponse;
    }

    public PumpsConfiguration getPumpsConfiguration() {
        final Semaphore semaphore = new Semaphore(0);
        List<PumpsConfiguration> pumpsConfigurations = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("pumpsConfiguration/");
        System.out.println("waiting for pump configurations from firebase...");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    PumpsConfiguration pumpsConfiguration = dataSnapshot.getValue(PumpsConfiguration.class);
                    pumpsConfigurations.add(pumpsConfiguration);
                    semaphore.release();
                } catch (Throwable t) {
                    throw t;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                throw error.toException();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return pumpsConfigurations.get(0);
    }

    public ServerResponse setPumpsConfiguration(PumpsConfiguration pumpsConfiguration) {
        Semaphore semaphore = new Semaphore(0);
        ServerResponse serverResponse = new ServerResponse();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("pumpsConfiguration");
        ref.setValue(pumpsConfiguration, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                serverResponse.successful = true;
                serverResponse.message = "pumps configuration updated successfully";
                semaphore.release();

            } else {
                serverResponse.successful = true;
                serverResponse.message = databaseError.getMessage();
                databaseError.toException().printStackTrace();
                semaphore.release();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return serverResponse;
    }

}
