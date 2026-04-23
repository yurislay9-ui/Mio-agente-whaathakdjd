package com.sponsorflow.agents.action

import android.content.Intent
import android.util.Log
import com.sponsorflow.agents.SponsorflowAgent
import com.sponsorflow.models.AgentResult
import com.sponsorflow.models.AgentTask
import com.sponsorflow.models.SquadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PUBLISHER AGENT (Escuadrón de Acción - El Publisher)
 * 
 * Sustituye completamente al viejo y acoplado `SocialMediaPublisher`.
 * Recibe una "ActionIntent" validada y se encarga de instanciar los Intents nativos
 * de Android (o llamar al Accesibility Mimic en Fases Posteriores).
 * Es un agente seguro contra fallos del Sistema Operativo.
 */
class PublisherAgent @Inject constructor() : SponsorflowAgent {
    private val TAG = "NEXUS_PublisherAgent"
    
    override val agentName: String = "PublisherAgent"
    override val squadron: SquadType = SquadType.ACTION
    override val capabilities: List<String> = listOf("social_media_posting", "ui_automation_wrapper")

    override suspend fun executeInternal(taskPayload: AgentTask): AgentResult {
        return withContext(Dispatchers.IO) {
            try {
                val action = taskPayload.proposedAction
                
                // Defensa contra acciones equivocadas (Safety Net)
                if (action == null || action.type != "PUBLISH_POST") {
                    return@withContext AgentResult.Failure("Acción vacía o tipo incorrecto")
                }

                val contentToPublish = action.payload
                val context = taskPayload.message.context

                Log.i(TAG, "📸 PublisherAgent iniciando secuencia de publicación limpia...")
                
                // SISTEMA ANTI-CRASH (SRE):
                // Si enviamos un intent y el usuario desinstaló Instagram o Facebook,
                // las apps monolíticas crashean con un "ActivityNotFoundException".
                // Este agente primero verifica la resolución segura del paquete.
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, contentToPublish)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "Sponsorflow: Selecciona red social").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Comprobamos si Android tiene alguna app capaz de manejar esta publicación
                if (shareIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(chooser)
                    Log.i(TAG, "✅ Panel delegado al OS (Modo Seguro).")
                    
                    AgentResult.Success(1.0, mapOf("publish_status" to "intent_fired"))
                } else {
                    Log.w(TAG, "⚠️ No hay aplicaciones disponibles para manejar la publicación. Abortando sin Crash.")
                    AgentResult.Failure("No hay app para manejar el Intent")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico protegido en PublisherAgent: ${e.message}")
                AgentResult.Failure("Excepción en PublisherAgent")
            }
        }
    }
}
