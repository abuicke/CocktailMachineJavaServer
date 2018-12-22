package lt.soe.cocktailmachineserver;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Firebase {

    public List<Cocktail> getCocktails() {
        final Semaphore semaphore = new Semaphore(0);
        List<Cocktail> cocktails = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    FirebaseCocktailSchema firebaseListing = snap.getValue(FirebaseCocktailSchema.class);
                    Cocktail cocktail = Cocktail.TEST_COCKTAIL(firebaseListing.name);
                    cocktails.add(cocktail);
                }
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                throw new IllegalStateException(error.toException());
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return cocktails;
    }

}
