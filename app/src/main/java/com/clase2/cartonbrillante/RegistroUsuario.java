package com.clase2.cartonbrillante;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistroUsuario extends AppCompatActivity {

    private EditText etNick, etNombreReal, etCorreo, etPass, etKonamiId, etNacimiento, etDireccion;
    private Spinner spPais;
    private TextView tvRegulacionAutomatica, tvTituloRegistro;
    private RadioGroup rgTipoCuenta;
    private LinearLayout llContenedorJuez, llContenedorTienda;
    private CheckBox cbTerminos, cbEsJuez;
    private Button btnRegistrarFinal;

    private boolean vieneDeGoogle = false;
    private DBHelper dbHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro_usuario);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);
        mAuth = FirebaseAuth.getInstance();

        // Enlazar las vistas
        tvTituloRegistro = findViewById(R.id.tvTituloRegistro);
        etNick = findViewById(R.id.etRegNick);
        etNombreReal = findViewById(R.id.etRegNombreReal);
        etCorreo = findViewById(R.id.etRegCorreo);
        etPass = findViewById(R.id.etRegPass);
        spPais = findViewById(R.id.spRegPais);
        tvRegulacionAutomatica = findViewById(R.id.tvRegulacionAutomatica);
        etKonamiId = findViewById(R.id.etRegKonamiId);
        etNacimiento = findViewById(R.id.etRegNacimiento);
        etDireccion = findViewById(R.id.etRegDireccion);

        rgTipoCuenta = findViewById(R.id.rgTipoCuenta);
        llContenedorJuez = findViewById(R.id.llContenedorJuez);
        llContenedorTienda = findViewById(R.id.llContenedorTienda);
        cbTerminos = findViewById(R.id.cbTerminos);
        cbEsJuez = findViewById(R.id.cbEsJuez);
        btnRegistrarFinal = findViewById(R.id.btnRegistrarFinal);

        // Configurar Spinner con la lista cerrada de países del mundo
        ArrayAdapter<String> adapterPaises = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                RegulacionHelper.LISTA_PAISES_MUNDO
        );
        spPais.setAdapter(adapterPaises);

        // Actualizar indicador TCG / OCG en tiempo real cuando cambia el país
        spPais.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String paisElegido = RegulacionHelper.LISTA_PAISES_MUNDO[position];
                String reg = RegulacionHelper.obtenerRegulacionPorPais(paisElegido);
                tvRegulacionAutomatica.setText("Regulación asignada por tu país (" + paisElegido + "): " + reg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Verificar si el usuario entró con el botón de Google
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("DESDE_GOOGLE", false)) {
            vieneDeGoogle = true;
            tvTituloRegistro.setText("Completa tu Perfil (Google)");

            String nombreGoogle = intent.getStringExtra("GOOGLE_NOMBRE");
            String correoGoogle = intent.getStringExtra("GOOGLE_CORREO");
            String nickSugerido = intent.getStringExtra("GOOGLE_NICK");
            String paisDetectado = intent.getStringExtra("GOOGLE_PAIS");

            if (nombreGoogle != null) etNombreReal.setText(nombreGoogle);
            if (nickSugerido != null) etNick.setText(nickSugerido);
            if (correoGoogle != null) {
                etCorreo.setText(correoGoogle);
                etCorreo.setEnabled(false);
            }
            etPass.setVisibility(View.GONE);

            if (paisDetectado != null) {
                for (int i = 0; i < RegulacionHelper.LISTA_PAISES_MUNDO.length; i++) {
                    if (RegulacionHelper.LISTA_PAISES_MUNDO[i].equalsIgnoreCase(paisDetectado)) {
                        spPais.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Mostrar/Ocultar campos según el Tipo de Cuenta
        rgTipoCuenta.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbIndividual) {
                llContenedorJuez.setVisibility(View.VISIBLE);
                llContenedorTienda.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbTienda) {
                llContenedorJuez.setVisibility(View.GONE);
                llContenedorTienda.setVisibility(View.VISIBLE);
                cbEsJuez.setChecked(false);
            }
        });

        cbTerminos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnRegistrarFinal.setEnabled(isChecked);
        });

        // Guardar Registro
        btnRegistrarFinal.setOnClickListener(v -> procesarRegistro());
    }

    private void procesarRegistro() {
        String nick = etNick.getText().toString().trim();
        String nombreReal = etNombreReal.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String paisSeleccionado = spPais.getSelectedItem().toString();
        String kid = etKonamiId.getText().toString().trim();
        String nacimiento = etNacimiento.getText().toString().trim();

        // 🟢 DEFINICIÓN DINÁMICA DEL ROL:
        String tipoCuenta;
        if (rgTipoCuenta.getCheckedRadioButtonId() == R.id.rbTienda) {
            tipoCuenta = "TIENDA";
        } else {
            tipoCuenta = cbEsJuez.isChecked() ? "JUEZ" : "JUGADOR";
        }

        if (nick.isEmpty() || nombreReal.isEmpty() || correo.isEmpty()) {
            Toast.makeText(this, "Completa tu Nickname, Nombre Real y Correo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!vieneDeGoogle && pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar localmente en SQLite
        long idGenerado = dbHelper.guardarUsuarioCompleto(nick, nombreReal, kid, paisSeleccionado, correo, pass, nacimiento, tipoCuenta);
        RegulacionHelper.guardarPaisUsuario(this, paisSeleccionado);

        if (idGenerado != -1) {
            SharedPreferences prefs = getSharedPreferences("CartonGoPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("id_usuario_activo", (int) idGenerado);
            editor.putString("rol_usuario_activo", tipoCuenta);
            editor.apply();
        }

        String regulacionAsignada = RegulacionHelper.obtenerRegulacionPorPais(paisSeleccionado);

        if (vieneDeGoogle) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String uid = currentUser != null ? currentUser.getUid() : correo;

            // Sincronizar inmediatamente con Cloud Firestore
            CloudSyncService.guardarUsuarioCloud(uid, nick, nombreReal, correo, paisSeleccionado, tipoCuenta);

            Toast.makeText(this, "¡Perfil completado como " + tipoCuenta + "! Regulación: " + regulacionAsignada, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MenuPrincipal.class));
            finish();
        } else {
            mAuth.createUserWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = user != null ? user.getUid() : correo;

                            // Sincronizar inmediatamente con Cloud Firestore
                            CloudSyncService.guardarUsuarioCloud(uid, nick, nombreReal, correo, paisSeleccionado, tipoCuenta);
                        }
                        Toast.makeText(this, "Cuenta registrada como " + tipoCuenta + " (" + paisSeleccionado + " • " + regulacionAsignada + ")", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, MenuPrincipal.class));
                        finish();
                    });
        }
    }
}