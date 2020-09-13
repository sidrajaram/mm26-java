package mech.mania.starter_pack

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import mech.mania.engine.domain.model.PlayerProtos.*
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.logging.Logger


/**
 * main function for running the server with no onReceive and onSend
 */
fun main(args: Array<String>) {
    Server().startServer(args[0].toInt(), {}, {})
    val latch = CountDownLatch(1)
    latch.await()
}

class Server {

    private val logger = Logger.getLogger(Server::class.toString())
    private val player: Strategy = PlayerStrategy()

    /**
     * Starts a server using a specified port
     * TODO: allow URL instead of port
     * @param port Port to start server on (localhost)
     * @param onReceive callback function that gets called when server receives turn
     * @param onSend callback function that gets called when server sends decision
     * @return non-zero if server fails to start, 0 if server starts properly
     */
    fun startServer(port: Int,
                    onReceive: (turn: PlayerTurn) -> Unit,
                    onSend: (decision: PlayerDecision) -> Unit): Int {
        try {
            // Create server on specified port
            HttpServer.create(InetSocketAddress(port), 0).apply {

                // Add handler to server endpoint which receives PlayerTurn and returns PlayerDecision
                createContext("/server") { exchange: HttpExchange ->
                    // read in input from server
                    // once the turn is parsed, use that turn to call a passed in function
                    val turn = PlayerTurn.parseFrom(exchange.requestBody)
                    logger.info("Received playerTurn: " + turn.playerName)
                    onReceive(turn)

                    // calculate what to do with turn
                    val decision: PlayerDecision = player.makeDecision(turn.playerName, turn.gameState)
                    val size: Long = decision.toByteArray().size.toLong()

                    // send back response
                    exchange.responseHeaders["Content-Type"] = "application/octet-stream"
                    exchange.sendResponseHeaders(200, size)
                    decision.writeTo(exchange.responseBody)
                    exchange.responseBody.flush()
                    exchange.responseBody.close()
                    logger.info("Sent playerDecision")
                    onSend(decision)
                }

                // Add handler to health endpoint which returns status code of 200
                createContext("/health") { exchange: HttpExchange ->
                    val message = "200".toByteArray()
                    exchange.sendResponseHeaders(200, message.size.toLong())
                    exchange.responseHeaders["Content-Type"] = "text/html"
                    exchange.responseBody.write(message)
                    exchange.responseBody.close()
                }

                // Start server
                start()
            }
            logger.info("Server started on port $port")
            return 0
        } catch (e: Exception) {
            logger.warning("Server failed to start on $port: ${e.message}")
            return 1
        }
    }
}
