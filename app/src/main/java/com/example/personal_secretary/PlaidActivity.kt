package com.example.personal_secretary

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties



class PlaidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaidScreen()
                }
            }
        }
    }
}


@Composable
fun PlaidScreen() {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf<List<PlaidTransaction>>(emptyList()) }
    var weeklyTotal by remember { mutableStateOf(0.0) }
    var monthlyTotal by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        loading = true
        error = null

        try {
            Log.d("PLAID_DEBUG", "Starting Plaid Sandbox data load...")
            val accessToken = createPlaidSandboxItem(context)
            Log.d("PLAID_DEBUG", "Access token retrieved.")

            val end = LocalDate.now()
            val start = end.minusDays(90)
            val fmt = DateTimeFormatter.ISO_DATE

            val list = getPlaidTransactionsWithRetry(
                context,
                accessToken,
                start.format(fmt),
                end.format(fmt)
            )

            Log.d("PLAID_DEBUG", "Downloaded ${list.size} transactions")


            transactions = list.sortedByDescending { it.date }
            weeklyTotal = computeWeeklyTotal(transactions)
            monthlyTotal = computeMonthlyTotal(transactions)

        } catch (e: Exception) {

            error = if (e.message == "Transactions product not ready after 10 attempts") {
                "Plaid transactions product did not become ready after multiple attempts. Try running again later."
            } else {
                e.localizedMessage ?: "Unknown error"
            }
            Log.e("PLAID_DEBUG", "Error loading sandbox data", e)
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Plaid Sandbox Demo", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Column(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Loading financial data (waiting for Plaid product readiness)...")
            }
        }

        error?.let { Text(it, color = Color.Red) }

        if (!loading && error == null) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryCard("Weekly total", weeklyTotal)
                SummaryCard("Monthly total", monthlyTotal)
            }

            Spacer(Modifier.height(8.dp))

            //Left space for my AI call later; will update this soon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.LightGray.copy(alpha = 0.2f), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Edit this text in code",
                    color = Color.DarkGray.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }


            Spacer(Modifier.height(12.dp))

            Text("Transactions", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}


@Composable
fun SummaryCard(label: String, amount: Double) {
    Column(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth(0.5f)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text("$${"%.2f".format(amount)}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@Composable
fun TransactionRow(tx: PlaidTransaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(tx.name, style = MaterialTheme.typography.bodyMedium)
                Text(tx.date, style = MaterialTheme.typography.bodySmall)
            }
            Text("$${"%.2f".format(tx.amount)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}


private fun Context.getPlaidConfig(key: String): String? {
    val props = Properties()
    return try {
        val input = resources.openRawResource(R.raw.config)
        props.load(input)
        input.close()
        props.getProperty(key)
    } catch (e: Exception) {
        Log.e("PLAID_DEBUG", "Config load error", e)
        null
    }
}

@Serializable
data class SandboxPublicTokenRequest(
    val client_id: String,
    val secret: String,
    val institution_id: String,
    val initial_products: List<String>
)

@Serializable
data class SandboxPublicTokenResponse(
    val public_token: String? = null,
    val item_id: String? = null,
    val request_id: String? = null
)

@Serializable
data class PublicTokenExchangeRequest(
    val client_id: String,
    val secret: String,
    val public_token: String
)

@Serializable
data class PublicTokenExchangeResponse(
    val access_token: String? = null,
    val item_id: String? = null,
    val request_id: String? = null
)

@Serializable
data class TransactionsGetRequest(
    val client_id: String,
    val secret: String,
    val access_token: String,
    val start_date: String,
    val end_date: String,
    val options: TransactionsGetOptions
)

@Serializable
data class TransactionsGetOptions(val count: Int, val offset: Int = 0)

@Serializable
data class TransactionsGetResponse(
    val transactions: List<PlaidTransactionRaw> = emptyList()
)

@Serializable
data class PlaidTransactionRaw(
    val name: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val category: List<String>? = null
)


@Serializable
data class PlaidErrorResponse(
    val error_code: String? = null,
    val error_message: String? = null,
    val error_type: String? = null,
    val display_message: String? = null
)


private suspend fun createPlaidSandboxItem(context: Context): String {
    val clientId = context.getPlaidConfig("PLAID_CLIENT_ID")!!
    val secret = context.getPlaidConfig("PLAID_SECRET")!!

    val jsonParser = Json { ignoreUnknownKeys = true }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(jsonParser) }
    }

    return try {
        val req = SandboxPublicTokenRequest(clientId, secret, "ins_109508", listOf("transactions"))
        val rawPublicToken = client.post("https://sandbox.plaid.com/sandbox/public_token/create") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.bodyAsText()

        //Logging of data received to figure out what serialiation to put above
        Log.d("PLAID_RAW", "Public token response: $rawPublicToken")

        val publicResp = jsonParser.decodeFromString<SandboxPublicTokenResponse>(rawPublicToken)

        val publicToken = publicResp.public_token
            ?: throw Exception("Plaid did not return public_token")

        val exchangeReq = PublicTokenExchangeRequest(clientId, secret, publicToken)
        val rawExchange = client.post("https://sandbox.plaid.com/item/public_token/exchange") {
            contentType(ContentType.Application.Json)
            setBody(exchangeReq)
        }.bodyAsText()

        Log.d("PLAID_RAW", "Exchange response: $rawExchange")

        val exchangeResp = jsonParser.decodeFromString<PublicTokenExchangeResponse>(rawExchange)

        exchangeResp.access_token ?: throw Exception("Plaid did not return access_token")

    } finally {
        client.close()
    }
}

private suspend fun getPlaidTransactions(
    context: Context,
    accessToken: String,
    startDate: String,
    endDate: String,
    count: Int = 500,
    offset: Int = 0
): List<PlaidTransaction> {

    val clientId = context.getPlaidConfig("PLAID_CLIENT_ID")!!
    val secret = context.getPlaidConfig("PLAID_SECRET")!!

    val jsonParser = Json { ignoreUnknownKeys = true }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(jsonParser) }
    }

    return try {
        val req = TransactionsGetRequest(clientId, secret, accessToken, startDate, endDate,
            TransactionsGetOptions(count.coerceIn(1, 500), offset))

        val httpResponse = client.post("https://sandbox.plaid.com/transactions/get") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }

        val raw = httpResponse.bodyAsText()

        Log.d("PLAID_RAW", "Transactions GET response: $raw")

        val errorCheck = jsonParser.decodeFromString<PlaidErrorResponse>(raw)

        if (errorCheck.error_code == "PRODUCT_NOT_READY") {
            throw Exception("PLAID_PRODUCT_NOT_READY")
        }

        val resp = jsonParser.decodeFromString<TransactionsGetResponse>(raw)

        resp.transactions.map {
            PlaidTransaction(it.name ?: "Unknown", it.amount ?: 0.0, it.date ?: "", it.category)
        }

    } finally {
        client.close()
    }
}

private suspend fun getPlaidTransactionsWithRetry(
    context: Context,
    accessToken: String,
    startDate: String,
    endDate: String
): List<PlaidTransaction> {
    var attempts = 0
    val maxAttempts = 10
    while (attempts < maxAttempts) {
        try {
            return getPlaidTransactions(context, accessToken, startDate, endDate)
        } catch (e: Exception) {
            // Check for the custom exception
            if (e.message == "PLAID_PRODUCT_NOT_READY") {
                attempts++
                Log.d("PLAID_DEBUG", "Transactions not ready, retrying... ($attempts/$maxAttempts)")
                delay(5000L)
            } else {
                throw e
            }
        }
    }
    throw Exception("Transactions product not ready after $maxAttempts attempts")
}

private fun computeWeeklyTotal(list: List<PlaidTransaction>): Double {
    val cutoff = LocalDate.now().minusDays(7)
    return list.filter { LocalDate.parse(it.date) >= cutoff }.sumOf { it.amount }
}

private fun computeMonthlyTotal(list: List<PlaidTransaction>): Double {
    val cutoff = LocalDate.now().minusMonths(1)
    return list.filter { LocalDate.parse(it.date) >= cutoff }.sumOf { it.amount }
}

data class PlaidTransaction(
    val name: String,
    val amount: Double,
    val date: String,
    val category: List<String>? = null
)