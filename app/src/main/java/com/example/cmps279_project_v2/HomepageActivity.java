package com.example.cmps279_project_v2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.firebase.database.ValueEventListener;

public class HomepageActivity extends AppCompatActivity {
    private DatabaseReference journalReference;
    private FirebaseAuth auth;
    private static final String TAG = "HomepageActivity";
    private SentimentApi sentimentApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ctHomepage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cmps279-project-v2-default-rtdb.asia-southeast1.firebasedatabase.app");
        journalReference = database.getReference("JournalEntry");
        auth = FirebaseAuth.getInstance();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        sentimentApi = retrofit.create(SentimentApi.class);
    }

    public void onClickHelp(View view) {
        TextView tvHelp = findViewById(R.id.tvHelptext);
        if (tvHelp != null) {
            String[] helpTexts = getResources().getStringArray(R.array.help_texts);
            int randomIndex = new Random().nextInt(helpTexts.length);
            tvHelp.setText(helpTexts[randomIndex]);
        }
    }

    public void onClickSaveJournal(View view) {
        EditText etJournal = findViewById(R.id.tiJournal);
        String journalText = etJournal.getText().toString().trim();

        if (!journalText.isEmpty()) {
            generateResponse(journalText);
            saveJournalEntry(journalText);
        } else {
            Toast.makeText(this, "Please write something before submitting.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveJournalEntry(String text) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference newJournalRef = journalReference.child(userId).push();
            newJournalRef.setValue(text).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(HomepageActivity.this, "Journal entry saved!", Toast.LENGTH_SHORT).show();
                    EditText etJournal = findViewById(R.id.tiJournal);
                } else {
                    Log.e(TAG, "Failed to save journal entry.", task.getException());
                    Toast.makeText(HomepageActivity.this, "Failed to save journal entry.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateResponse(String text) {
        SentimentRequest request = new SentimentRequest(text);
        Call<List<SentimentResponse>> call = sentimentApi.analyzeSentiment(request);
        call.enqueue(new Callback<List<SentimentResponse>>() {
            @Override
            public void onResponse(Call<List<SentimentResponse>> call, Response<List<SentimentResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SentimentResponse> result = response.body();
                    handleSentimentResult(result);
                } else {
                    Log.e(TAG, "Response was not successful.");
                    Toast.makeText(HomepageActivity.this, "Failed to get analysis result.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SentimentResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to get response from backend.", t);
                Toast.makeText(HomepageActivity.this, "Error contacting the backend.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void handleSentimentResult(List<SentimentResponse> result) {
        String message = "Sentiment analysis result could not be determined.";
        float positiveScore = 0;
        float neutralScore = 0;
        float negativeScore = 0;

        // Extract scores for each sentiment
        for (SentimentResponse sentiment : result) {
            switch (sentiment.getLabel()) {
                case "POS":
                    positiveScore = sentiment.getScore();
                    break;
                case "NEU":
                    neutralScore = sentiment.getScore();
                    break;
                case "NEG":
                    negativeScore = sentiment.getScore();
                    break;
            }
        }

        if (positiveScore > neutralScore && positiveScore > negativeScore) {
            message = "Sounds like you had a great day, you should reflect on this day in order to learn what made you happy and reinforce that behavior.";
        } else if (negativeScore > positiveScore && negativeScore > neutralScore) {
            message = "Everyone has bad days, you can't let yourself be hung up on them. Avoid what made you feel negatively and focus more on developing healthy behaviors.";
        } else {
            message = "It seems like your day was neutral. It's okay to have days that are neither particularly good nor bad.";
        }

        Log.d(TAG, "Positive Score: " + positiveScore + ", Neutral Score: " + neutralScore + ", Negative Score: " + negativeScore);

        EditText etJournal = findViewById(R.id.tiJournal);
        etJournal.setText(message);
    }
    public void onClickGetJournal(View view) {
        setContentView(R.layout.entries);
        TextView tvFeedback = findViewById(R.id.feedbackTV);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userJournalRef = journalReference.child(userId);

            userJournalRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    StringBuilder feedbackText = new StringBuilder();
                    for (DataSnapshot entrySnapshot : dataSnapshot.getChildren()) {
                        String entry = entrySnapshot.getValue(String.class);
                        feedbackText.append(entry).append("\n\n");
                    }
                    tvFeedback.setText(feedbackText.toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Failed to read journal entries.", databaseError.toException());
                    Toast.makeText(HomepageActivity.this, "Failed to load journal entries.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }
    }
    public void onClickClear(View view){
        EditText journal = findViewById(R.id.tiJournal);
        journal.setText("");
    }

    public void onClickBacktoLP(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void onClickBackToHP(View view) {
        setContentView(R.layout.activity_homepage);
    }

    public void onClickLanguage(View view) {
        setContentView(R.layout.language);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
