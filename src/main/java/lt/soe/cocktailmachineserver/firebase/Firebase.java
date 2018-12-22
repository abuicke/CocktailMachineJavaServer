package lt.soe.cocktailmachineserver.firebase;

import com.google.firebase.database.*;
import lt.soe.cocktailmachineserver.cocktail.Cocktail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Firebase {

    public List<Cocktail> getCocktails() {
        final Semaphore semaphore = new Semaphore(0);
        List<Cocktail> cocktails = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/");
        System.out.println("waiting for data from firebase...");
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

}
