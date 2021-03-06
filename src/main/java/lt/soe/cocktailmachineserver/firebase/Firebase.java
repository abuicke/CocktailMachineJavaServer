package lt.soe.cocktailmachineserver.firebase;

import com.google.firebase.database.*;
import lt.soe.cocktailmachineserver.cocktail.Cocktail;
import lt.soe.cocktailmachineserver.pumps.PumpsConfiguration;
import lt.soe.cocktailmachineserver.systemstate.SystemEvent;
import lt.soe.cocktailmachineserver.systemstate.SystemEventsQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Firebase {

    public List<Cocktail> getCocktails() {
        final Semaphore semaphore = new Semaphore(0);
        List<Cocktail> cocktails = new ArrayList<>();

        System.out.println("waiting for cocktails from firebase...");
        SystemEventsQueue.add(new SystemEvent("waiting for cocktails from firebase..."));
        // Get a reference for all cocktails in Firebase, i.e.
        // |-----------|
        // |cocktails/ |
        // |...        |
        // |...........|
        // pumpsConfigurations/
        // ...
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/");
        // Set a listener to get data from Firebase database
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Loop through every cocktail in the database
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    // Create the cocktail object from data in firebase
                    Cocktail cocktail = snap.getValue(Cocktail.class);
                    cocktails.add(cocktail);
                }
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("get cocktails from firebase failed");
                SystemEventsQueue.add(new SystemEvent("get cocktails from firebase failed", true));
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

    public boolean constructCocktail(int index, Cocktail cocktail) {
        Semaphore semaphore = new Semaphore(0);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference ref = firebaseDatabase.getReference("cocktails/" + index);
        List<Boolean> isSuccessful = new ArrayList<>();
        ref.setValue(cocktail, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                SystemEventsQueue.add(new SystemEvent(
                        "added cocktail " + cocktail.name + " to firebase successfully"));
                System.out.println("added cocktail " + cocktail.name + " to firebase successfully");
                isSuccessful.add(true);
            } else {
                SystemEventsQueue.add(new SystemEvent(
                        "firebase database error " + databaseError.getMessage(), true));
                databaseError.toException().printStackTrace();
                isSuccessful.add(false);
            }

            semaphore.release();
        });

        try {
            semaphore.acquire();
            return isSuccessful.get(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
            SystemEventsQueue.add(new SystemEvent(
                    "failed to construct cocktail " + cocktail, true));
            if (isSuccessful.isEmpty()) {
                return false;
            } else {
                return isSuccessful.get(0);
            }
        }
    }

    public void deleteCocktail(int index) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/");
        ref.child(Integer.toString(index)).removeValue((databaseError, databaseReference) -> {
            System.out.println("deleted " + databaseReference + ", error: " + databaseError);
        });
    }

    public PumpsConfiguration getPumpsConfiguration() {
        // This is a synchronization lock, it stops other code from changing the pumps while
        // the pump data is being read. This is so the state of the pumps doesn't change
        // before it is read, e.g. if a drink is made while the pump configuration is
        // being read than the wrong information will be returned.
        final Semaphore semaphore = new Semaphore(0);
        // Create an empty List object to store the pump configuration in.
        List<PumpsConfiguration> pumpsConfigurations = new ArrayList<>();

        // Get reference to pump configuration data in Firebase.
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("pumpsConfiguration/");
        System.out.println("waiting for pumps configuration from firebase...");
        SystemEventsQueue.add(new SystemEvent("waiting for pumps configuration from firebase..."));
        // Get pump configuration data from Firebase
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PumpsConfiguration pumpsConfiguration = dataSnapshot.getValue(PumpsConfiguration.class);
                pumpsConfigurations.add(pumpsConfiguration);
                // Release the lock now that the data has been received.
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                semaphore.release();
                error.toException().printStackTrace();
                SystemEventsQueue.add(new SystemEvent(
                        "failed to load pumps configuration from " +
                                "firebase database " + error.getMessage(), true));
            }
        });

        try {
            // Turn on the synchronization lock
            semaphore.acquire();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            SystemEventsQueue.add(new SystemEvent(
                    "failed to load pumps configuration from firebase database", true));
        }

        // Return the pump configuration.
        return pumpsConfigurations.get(0);
    }

    public boolean setPumpsConfiguration(PumpsConfiguration pumpsConfiguration) {
        Semaphore semaphore = new Semaphore(0);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("pumpsConfiguration");
        List<Boolean> isSuccessful = new ArrayList<>();
        ref.setValue(pumpsConfiguration, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                isSuccessful.add(true);
                SystemEventsQueue.add(new SystemEvent(
                        "pumps configuration updated successfully"));
            } else {
                isSuccessful.add(false);
                SystemEventsQueue.add(new SystemEvent(
                        "failed to update pumps configuration in firebase " +
                                "database " + databaseError.getMessage(), true));
                databaseError.toException().printStackTrace();
            }

            semaphore.release();
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            SystemEventsQueue.add(new SystemEvent(
                    "failed to update pumps configuration " +
                            "in firebase database", true));
            if (isSuccessful.isEmpty()) {
                return false;
            } else {
                return isSuccessful.get(0);
            }
        }

        return isSuccessful.get(0);
    }

}
