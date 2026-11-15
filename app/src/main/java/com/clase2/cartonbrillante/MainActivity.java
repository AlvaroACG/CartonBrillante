package com.clase2.cartonbrillante;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DBHelper dbHelper;
    private FirebaseFirestore db;
    Button btnIngresar, btnIrRegistro, btnGoogleLogin;
    EditText edtLogin, edtPass;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null && account.getIdToken() != null) {
                        firebaseAuthWithGoogle(account);
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "Error Google Sign-In (" + e.getStatusCode() + "): "
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicializar Firestore y configurar la persistencia offline correctamente dentro de onCreate
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Habilita la caché offline nativa
                .build();
        db.setFirestoreSettings(settings);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DBHelper(this);

        // 2. Validación de sesión activa (Offline-first: si ya inició sesión antes, entra directo)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String correoActual = currentUser.getEmail();
            if (correoActual != null) {
                String paisGuardado = dbHelper.obtenerPaisPorCorreo(correoActual);
                if (paisGuardado != null) {
                    RegulacionHelper.guardarPaisUsuario(this, paisGuardado);
                }
            }
            startActivity(new Intent(this, MenuPrincipal.class));
            finish();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnIngresar = findViewById(R.id.btnIngresar);
        btnIrRegistro = findViewById(R.id.btnIrRegistro);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        edtLogin = findViewById(R.id.etLoginUsuario);
        edtPass = findViewById(R.id.etLoginPass);

        btnIngresar.setOnClickListener(v -> {
            String email = edtLogin.getText().toString().trim();
            String password = CONFIGURE_LOCALLY;
            if (!email.isEmpty() && !password.isEmpty()) {
                realizarLogin(email, password);
            } else {
                Toast.makeText(this, "Ingresa tus datos", Toast.LENGTH_SHORT).show();
            }
        });

        btnIrRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistroUsuario.class);
            startActivity(intent);
        });

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void realizarLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String paisGuardado = dbHelper.obtenerPaisPorCorreo(email);
                            if (paisGuardado != null) {
                                RegulacionHelper.guardarPaisUsuario(MainActivity.this, paisGuardado);
                            }
                            Toast.makeText(MainActivity.this, "Acceso concedido", Toast.LENGTH_SHORT).show();
                            Intent i = new Intent(MainActivity.this, MenuPrincipal.class);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String correo = (user != null && user.getEmail() != null) ? user.getEmail() : account.getEmail();
                        String nombreCompleto = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : account.getDisplayName();
                        String nickSugerido = (account.getGivenName() != null) ? account.getGivenName() : nombreCompleto;

                        String paisDetectado = Locale.getDefault().getDisplayCountry(new Locale("es", "ES"));

                        if (dbHelper.existeUsuarioPorCorreo(correo)) {
                            String paisUsuario = dbHelper.obtenerPaisPorCorreo(correo);
                            if (paisUsuario != null) {
                                RegulacionHelper.guardarPaisUsuario(this, paisUsuario);
                            }
                            Toast.makeText(this, "Bienvenido de vuelta " + nombreCompleto, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MenuPrincipal.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Completa los datos restantes de tu cuenta", Toast.LENGTH_LONG).show();
                            Intent intentReg = new Intent(this, RegistroUsuario.class);
                            intentReg.putExtra("DESDE_GOOGLE", true);
                            intentReg.putExtra("GOOGLE_NOMBRE", nombreCompleto);
                            intentReg.putExtra("GOOGLE_CORREO", correo);
                            intentReg.putExtra("GOOGLE_NICK", nickSugerido);
                            intentReg.putExtra("GOOGLE_PAIS", paisDetectado);
                            startActivity(intentReg);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error en autenticación de Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}