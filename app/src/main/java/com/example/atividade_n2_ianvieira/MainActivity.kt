package com.example.atividade_n2_ianvieira

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.example.atividaden2_ianvieira.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var tvAppTitle: TextView
    private lateinit var btnToggleLanguage: Button
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var etWord: TextInputEditText
    private lateinit var btnMic: Button
    private lateinit var btnSearch: Button
    private lateinit var tvWordResult: TextView
    private lateinit var tvPhonetic: TextView
    private lateinit var tvPartOfSpeech: TextView
    private lateinit var tvDefinition: TextView

    private var isEnglish = true

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var speechResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAppTitle = findViewById(R.id.tvAppTitle)
        btnToggleLanguage = findViewById(R.id.btnToggleLanguage)
        textInputLayout = findViewById(R.id.textInputLayout)
        etWord = findViewById(R.id.etWord)
        btnMic = findViewById(R.id.btnMic)
        btnSearch = findViewById(R.id.btnSearch)
        tvWordResult = findViewById(R.id.tvWordResult)
        tvPhonetic = findViewById(R.id.tvPhonetic)
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech)
        tvDefinition = findViewById(R.id.tvDefinition)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val msgConcedida = if (isEnglish) "Permission granted! Starting mic..." else "Permissão concedida! Abrindo microfone..."
                Toast.makeText(this, msgConcedida, Toast.LENGTH_SHORT).show()
                iniciarReconhecimentoDeVoz()
            } else {
                val msgNegada = if (isEnglish) "Mic access denied. You can still type." else "Microfone negado. Você ainda pode digitar."
                Toast.makeText(this, msgNegada, Toast.LENGTH_LONG).show()
            }
        }

        speechResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                if (!spokenText.isNullOrEmpty()) {
                    etWord.setText(spokenText)
                    dispararBuscaVisual(spokenText)
                }
            }
        }

        btnToggleLanguage.setOnClickListener {
            isEnglish = !isEnglish
            atualizarTextosDaInterface()
        }

        btnMic.setOnClickListener {
            verificarPermissaoMicrofone()
        }

        btnSearch.setOnClickListener {
            val word = etWord.text.toString().trim()
            if (word.isEmpty()) {
                val msgVazio = if (isEnglish) "Please, type a word!" else "Por favor, digite uma palavra!"
                Toast.makeText(this, msgVazio, Toast.LENGTH_SHORT).show()
            } else {
                dispararBuscaVisual(word)
            }
        }
    }

    private fun verificarPermissaoMicrofone() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                iniciarReconhecimentoDeVoz()
            }
            else -> {
                val msgExplicacao = if (isEnglish) "We need microphone access to search by voice." else "Precisamos do microfone para busca por voz."
                Toast.makeText(this, msgExplicacao, Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun iniciarReconhecimentoDeVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // Força a escuta em inglês
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, if (isEnglish) "Speak an English word" else "Diga uma palavra em inglês")

        try {
            speechResultLauncher.launch(intent)
        } catch (e: Exception) {
            val erroVoz = if (isEnglish) "Voice feature not supported." else "Reconhecimento de voz não suportado."
            Toast.makeText(this, erroVoz, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispararBuscaVisual(word: String) {
        btnSearch.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E53935"))
        btnSearch.text = if (isEnglish) "Searching..." else "Buscando..."
        btnSearch.isEnabled = false
        btnMic.isEnabled = false
        buscarPalavra(word)
    }

    private fun atualizarTextosDaInterface() {
        if (isEnglish) {
            btnToggleLanguage.text = "PT-BR"
            tvAppTitle.text = "English Dictionary"
            textInputLayout.hint = "Type a word in English"
            btnSearch.text = "Search Word"

            if (tvPhonetic.text == "/.../") {
                tvWordResult.text = "Waiting for a word..."
                tvPartOfSpeech.text = "Part of Speech: -"
                tvDefinition.text = "Definition: -"
            }
        } else {
            btnToggleLanguage.text = "EN"
            tvAppTitle.text = "Dicionário de Inglês"
            textInputLayout.hint = "Digite uma palavra em inglês"
            btnSearch.text = "Buscar Palavra"

            if (tvPhonetic.text == "/.../") {
                tvWordResult.text = "Aguardando uma palavra..."
                tvPartOfSpeech.text = "Classe Gramatical: -"
                tvDefinition.text = "Definição: -"
            }
        }
    }

    private fun restaurarBotao() {
        btnSearch.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#673AB7"))
        btnSearch.text = if (isEnglish) "Search Word" else "Buscar Palavra"
        btnSearch.isEnabled = true
        btnMic.isEnabled = true
    }

    private fun buscarPalavra(word: String) {
        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}"
        val queue = Volley.newRequestQueue(this)

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val firstEntry = response.getJSONObject(0)
                    val wordText = firstEntry.getString("word")
                    val phonetic = firstEntry.optString("phonetic", "Phonetic not available")

                    val meanings = firstEntry.getJSONArray("meanings")
                    val firstMeaning = meanings.getJSONObject(0)
                    val partOfSpeech = firstMeaning.getString("partOfSpeech")

                    val definitionsArray = firstMeaning.getJSONArray("definitions")
                    val definicoesJuntas = StringBuilder()

                    val limite = if (definitionsArray.length() > 3) 3 else definitionsArray.length()
                    for (i in 0 until limite) {
                        val defObj = definitionsArray.getJSONObject(i)
                        val textoDefinicao = defObj.getString("definition")
                        definicoesJuntas.append("${i + 1}. $textoDefinicao\n\n")
                    }

                    val labelPart = if (isEnglish) "Part of Speech: " else "Classe: "
                    val labelDef = if (isEnglish) "Definitions:\n" else "Definições:\n"

                    tvWordResult.text = wordText.replaceFirstChar { it.uppercase() }
                    tvPhonetic.text = phonetic
                    tvPartOfSpeech.text = "$labelPart${partOfSpeech.uppercase()}"
                    tvDefinition.text = "$labelDef${definicoesJuntas.toString().trim()}"

                    restaurarBotao()

                } catch (e: Exception) {
                    val msgErroJson = if (isEnglish) "Error reading data." else "Erro ao ler dados."
                    Toast.makeText(this, msgErroJson, Toast.LENGTH_SHORT).show()
                    restaurarBotao()
                }
            },
            { error ->
                val msgNaoEncontrada = if (isEnglish) "Word not found! Try another one." else "Palavra não encontrada! Tente outra."
                Toast.makeText(this, msgNaoEncontrada, Toast.LENGTH_LONG).show()

                val textoVazio = if (isEnglish) "Word not found" else "Palavra não encontrada"
                tvWordResult.text = textoVazio
                tvPhonetic.text = "/.../"
                tvPartOfSpeech.text = if (isEnglish) "Part of Speech: -" else "Classe Gramatical: -"
                tvDefinition.text = if (isEnglish) "Definitions: -" else "Definições: -"

                restaurarBotao()
            }
        )
        queue.add(request)
    }
}