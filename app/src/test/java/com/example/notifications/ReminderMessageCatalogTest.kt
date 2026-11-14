package com.example.notifications

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMessageCatalogTest {

  @Test
  fun catalog_hasAtLeast30DistinctMessagesWithFormalMajority() {
    val templates = ReminderMessageCatalog.templates
    assertTrue("Expected at least 30 templates", templates.size >= 30)

    val formalCount = templates.count { it.tone == ReminderTone.FORMAL }
    val playfulCount = templates.count { it.tone == ReminderTone.PLAYFUL }
    assertTrue("Formal templates should be the majority", formalCount > playfulCount)
    assertTrue("Playful templates should exist", playfulCount >= 8)

    val distinctBodies =
      templates.map { it.bodyBuilder("Reserva de Emergência", "Cartão Nubank") }.toSet()
    assertEquals(templates.size, distinctBodies.size)
  }

  @Test
  fun pickMessage_usesGoalAndDebtNamesWhenAvailableAndSafeFallbacksWhenEmpty() {
    val goals = listOf("Viagem Escócia")
    val debts = listOf("Empréstimo Banco")

    val sampledWithContext =
      (0 until 150).map {
        ReminderMessageCatalog.pickMessage(goals, debts, lastIndex = -1, random = Random(it))
      }
    assertTrue(sampledWithContext.any { it.body.contains("Viagem Escócia") })
    assertTrue(sampledWithContext.any { it.body.contains("Empréstimo Banco") })

    val sampledWithoutContext =
      (0 until 150).map {
        ReminderMessageCatalog.pickMessage(
          emptyList(),
          emptyList(),
          lastIndex = -1,
          random = Random(it),
        )
      }
    sampledWithoutContext.forEach { msg ->
      assertTrue(msg.title.isNotBlank())
      assertTrue(msg.body.isNotBlank())
      assertFalse(msg.body.contains("null"))
      assertFalse(msg.body.contains("\"\""))
    }
  }
}
