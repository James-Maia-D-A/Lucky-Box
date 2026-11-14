package com.example.notifications

import kotlin.random.Random

enum class ReminderTone {
  FORMAL,
  PLAYFUL,
}

enum class ReminderTarget {
  GENERAL,
  GOAL,
  DEBT,
}

data class ReminderTemplate(
  val id: Int,
  val tone: ReminderTone,
  val target: ReminderTarget,
  val title: String,
  val bodyBuilder: (goal: String?, debt: String?) -> String,
)

data class RenderedReminder(
  val index: Int,
  val tone: ReminderTone,
  val title: String,
  val body: String,
)

/**
 * Catálogo com 36 variações de notificações diárias (24 formais e 12 descontraídas),
 * utilizando os objetivos e dívidas reais do usuário quando disponíveis.
 */
object ReminderMessageCatalog {

  val templates: List<ReminderTemplate> =
    listOf(
      // --- 24 VARIAÇÕES FORMAIS (~67% de maioria) ---
      ReminderTemplate(
        id = 1,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Atualização do Livro Caixa",
        bodyBuilder = { _, _ ->
          "Registre suas entradas e saídas de hoje no Livro Caixa para manter suas médias financeiras precisas."
        },
      ),
      ReminderTemplate(
        id = 2,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Controle financeiro em dia",
        bodyBuilder = { _, _ ->
          "Dedique um minuto para lançar as movimentações recentes no Lucky Box. Dados atualizados geram projeções mais confiáveis."
        },
      ),
      ReminderTemplate(
        id = 3,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Compromisso financeiro pendente",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Lembre-se de que você possui a dívida \"$debt\" em aberto. Mantenha o Livro Caixa atualizado para organizar sua quitação."
          } else {
            "Manter cada entrada e saída registrada garante previsibilidade para quitar compromissos e proteger seu saldo."
          }
        },
      ),
      ReminderTemplate(
        id = 4,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Acompanhamento de objetivo",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Atualize seu fluxo de caixa hoje para acompanhar exatamente quanto falta para alcançar \"$goal\"."
          } else {
            "Preencha seu Livro Caixa diariamente para acompanhar com clareza a evolução do seu saldo rumo às suas metas."
          }
        },
      ),
      ReminderTemplate(
        id = 5,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Relatório de fluxo de caixa",
        bodyBuilder = { _, _ ->
          "Pequenos gastos não anotados distorcem sua média mensal e a divisão 50/30/20. Atualize seu histórico agora."
        },
      ),
      ReminderTemplate(
        id = 6,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Planejamento de parcelas",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Sua capacidade de amortizar \"$debt\" depende da precisão do seu caixa. Anote os lançamentos de hoje no app."
          } else {
            "A reserva mensal de 20% para dívidas e investimentos é recalculada automaticamente a cada novo registro."
          }
        },
      ),
      ReminderTemplate(
        id = 7,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Progresso rumo à meta",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "O prazo estimado para conquistar \"$goal\" depende dos seus registros atuais. Já preencheu o Livro Caixa hoje?"
          } else {
            "Cada lançamento correto melhora a precisão do gráfico de crescimento e dos prazos dos seus objetivos."
          }
        },
      ),
      ReminderTemplate(
        id = 8,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Conferência diária de caixa",
        bodyBuilder = { _, _ ->
          "Antes que o dia termine, revise seus recebimentos e pagamentos e registre-os no Lucky Box."
        },
      ),
      ReminderTemplate(
        id = 9,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Precisão nas médias de gastos",
        bodyBuilder = { _, _ ->
          "Suas médias diária, semanal, mensal e anual refletem apenas o que é registrado. Mantenha seus dados completos."
        },
      ),
      ReminderTemplate(
        id = 10,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Atenção ao orçamento mensal",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Não deixe o controle escapar: registre suas despesas de hoje para manter o plano de pagamento de \"$debt\" no rumo certo."
          } else {
            "Manter a disciplina no preenchimento do Livro Caixa evita surpresas no fechamento do mês."
          }
        },
      ),
      ReminderTemplate(
        id = 11,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Compromisso com seu objetivo",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Esquecer de registrar gastos pode atrasar a conquista de \"$goal\". Atualize seu Livro Caixa em poucos segundos."
          } else {
            "A falta de registros contínuos pode comprometer suas metas financeiras. Reserve um instante para atualizar o app."
          }
        },
      ),
      ReminderTemplate(
        id = 12,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Divisão 50/30/20 atualizada",
        bodyBuilder = { _, _ ->
          "Sua sugestão inteligente de distribuição mensal (Essencial, Lazer e Investimentos) depende dos dados de hoje."
        },
      ),
      ReminderTemplate(
        id = 13,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Lembrete de escrituração",
        bodyBuilder = { _, _ ->
          "Anote agora qualquer compra, transferência ou receita recebida para evitar esquecimentos posteriores."
        },
      ),
      ReminderTemplate(
        id = 14,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Gestão de passivos",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Para reduzir o prazo de pagamento de \"$debt\", mantenha todas as entradas e saídas devidamente lançadas."
          } else {
            "Controle rigoroso das saídas aumenta sua sobra mensal e acelera a quitação de obrigações financeiras."
          }
        },
      ),
      ReminderTemplate(
        id = 15,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Visibilidade das suas metas",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Confira quanto falta para completar \"$goal\" após atualizar suas movimentações financeiras de hoje."
          } else {
            "Acompanhar o crescimento do seu caixa começa pelo registro pontual de cada movimentação."
          }
        },
      ),
      ReminderTemplate(
        id = 16,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Previsibilidade financeira",
        bodyBuilder = { _, _ ->
          "Projeções dos próximos meses funcionam melhor quando o mês atual está rigorosamente preenchido."
        },
      ),
      ReminderTemplate(
        id = 17,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Registro rápido no Lucky Box",
        bodyBuilder = { _, _ ->
          "Teve alguma despesa ou entrada nas últimas horas? Lance no Livro Caixa enquanto os valores estão frescos na memória."
        },
      ),
      ReminderTemplate(
        id = 18,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Equilíbrio entre caixa e dívidas",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Lembre-se de \"$debt\": acompanhar cada gasto no app protege a sobra mensal destinada às suas parcelas."
          } else {
            "Registrar suas despesas diariamente fortalece sua margem mensal para investimentos e amortização de dívidas."
          }
        },
      ),
      ReminderTemplate(
        id = 19,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Consistência financeira",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "A constância nos registros aproxima você de \"$goal\". Atualize seu Livro Caixa agora."
          } else {
            "A consistência diária no preenchimento da planilha é o principal fator para alcançar estabilidade financeira."
          }
        },
      ),
      ReminderTemplate(
        id = 20,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Auditoria pessoal do dia",
        bodyBuilder = { _, _ ->
          "Faça uma breve revisão do seu extrato ou carteira e atualize o Lucky Box para fechar este período sem lacunas."
        },
      ),
      ReminderTemplate(
        id = 21,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Dados reais, decisões melhores",
        bodyBuilder = { _, _ ->
          "Decisões financeiras seguras exigem números reais. Verifique se há lançamentos pendentes no seu Livro Caixa."
        },
      ),
      ReminderTemplate(
        id = 22,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.DEBT,
        title = "Monitoramento de dívidas",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Caso já tenha abatido parte de \"$debt\" ou tido novos gastos hoje, registre tudo no Lucky Box."
          } else {
            "Mantenha o registro de pagamentos e entradas atualizado para recalcular suas sugestões de parcelamento."
          }
        },
      ),
      ReminderTemplate(
        id = 23,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GOAL,
        title = "Proteja seus objetivos",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Não comprometa a realização de \"$goal\" por falta de controle: mantenha seu fluxo de caixa em dia."
          } else {
            "Sem acompanhamento contínuo, os gastos invisíveis comprometem o orçamento. Atualize seu Livro Caixa."
          }
        },
      ),
      ReminderTemplate(
        id = 24,
        tone = ReminderTone.FORMAL,
        target = ReminderTarget.GENERAL,
        title = "Lembrete programado do caixa",
        bodyBuilder = { _, _ ->
          "Seu turno de atualização chegou. Acesse o Lucky Box e confirme se todas as entradas e saídas estão registradas."
        },
      ),

      // --- 12 VARIAÇÕES DESCONTRAÍDAS / INSPIRADAS NO DUOLINGO (~33%) ---
      ReminderTemplate(
        id = 25,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.DEBT,
        title = "Ei, o duende está de olho!",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Lembre que tem que pagar \"$debt\", então não esquece de anotar o que entra e sai aqui no app!"
          } else {
            "Fingir que aquele gasto não existiu não faz ele sumir do extrato. Bora anotar no Livro Caixa!"
          }
        },
      ),
      ReminderTemplate(
        id = 26,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GOAL,
        title = "E aí, já anotou hoje?",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "E aí, já preencheu o app hoje para ficar mais perto de \"$goal\"? O pote de ouro não se enche sozinho!"
          } else {
            "O duende ajustou o monóculo e viu seu Livro Caixa parado. Passa lá antes que a sorte tire folga!"
          }
        },
      ),
      ReminderTemplate(
        id = 27,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GENERAL,
        title = "Sem mágica sem registro!",
        bodyBuilder = { _, _ ->
          "Nem um Leprechaun consegue adivinhar onde foi parar o seu dinheiro se você não lançar no Livro Caixa."
        },
      ),
      ReminderTemplate(
        id = 28,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GOAL,
        title = "Vai deixar seu objetivo esperando?",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Você prometeu conquistar \"$goal\", lembra? Um minutinho no Lucky Box e você não perde o ritmo!"
          } else {
            "Aquele cafezinho e aquele Pix também contam! Atualiza o app agora pra não levar susto no fim do mês."
          }
        },
      ),
      ReminderTemplate(
        id = 29,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.DEBT,
        title = "Alerta de monóculo levantado!",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Se você esquecer de atualizar o caixa hoje, \"$debt\" pode acabar dando risada por último. Abre o app!"
          } else {
            "Deixar o Livro Caixa para depois é o jeito mais rápido de se perguntar: \"Para onde foi meu salário?\""
          }
        },
      ),
      ReminderTemplate(
        id = 30,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GENERAL,
        title = "Toc toc... é o seu dinheiro!",
        bodyBuilder = { _, _ ->
          "Só passando para avisar que ignorar essa notificação não cancela os gastos do dia. Anota tudo no Lucky Box!"
        },
      ),
      ReminderTemplate(
        id = 31,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GOAL,
        title = "O trevo quer ver progresso!",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Cada centavo anotado te aproxima de \"$goal\". Vai mesmo deixar seu objetivo no vácuo hoje?"
          } else {
            "Sua planilha inteligente está com saudade de um lançamento novinho. Leva menos de 15 segundos!"
          }
        },
      ),
      ReminderTemplate(
        id = 32,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GENERAL,
        title = "Promessa é dívida (literalmente)",
        bodyBuilder = { _, _ ->
          "O duende não queria pressionar, mas se você não preencher hoje, suas médias vão ficar de cara feia."
        },
      ),
      ReminderTemplate(
        id = 33,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.DEBT,
        title = "Missão anti-sufoco!",
        bodyBuilder = { _, debt ->
          if (debt != null) {
            "Quer ver \"$debt\" sumir do mapa? Então abre o Lucky Box e registra o que entrou e saiu hoje!"
          } else {
            "Quer sobrar dinheiro no final do mês sem parecer milagre? Começa anotando os gastos de hoje!"
          }
        },
      ),
      ReminderTemplate(
        id = 34,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GENERAL,
        title = "Olha o gráfico te chamando!",
        bodyBuilder = { _, _ ->
          "Seu gráfico de crescimento está esperando o registro de hoje para decidir se sobe ou se chora. Atualiza lá!"
        },
      ),
      ReminderTemplate(
        id = 35,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GOAL,
        title = "Não brinque com a sorte!",
        bodyBuilder = { goal, _ ->
          if (goal != null) {
            "Esquecer o Livro Caixa hoje é dar um passo para longe de \"$goal\". Dá um pulo no app rapidinho!"
          } else {
            "Até o duende anota cada moeda de ouro que entra no baú. Qual é a sua desculpa hoje?"
          }
        },
      ),
      ReminderTemplate(
        id = 36,
        tone = ReminderTone.PLAYFUL,
        target = ReminderTarget.GENERAL,
        title = "Check-in do baú da sorte!",
        bodyBuilder = { _, _ ->
          "Passou o cartão ou recebeu um troco? Antes de esquecer o valor, joga no Lucky Box e fica tranquilo!"
        },
      ),
    )

  fun pickMessage(
    goalNames: List<String>,
    debtNames: List<String>,
    lastIndex: Int = -1,
    random: Random = Random.Default,
  ): RenderedReminder {
    // Prioriza mensagens personalizadas quando o usuário possui objetivos ou dívidas ativos,
    // mantendo a proporção majoritariamente formal (~70% formal, ~30% descontraída).
    val wantPlayful = random.nextFloat() < 0.30f
    val tonePool =
      templates.withIndex().filter {
        it.value.tone == if (wantPlayful) ReminderTone.PLAYFUL else ReminderTone.FORMAL
      }

    // Filtra candidatos evitando repetir imediatamente o último índice e dando leve preferência
    // a templates que utilizam objetivos/dívidas caso existam.
    val validCandidates =
      tonePool.filter { (idx, tmpl) ->
        idx != lastIndex &&
          when (tmpl.target) {
            ReminderTarget.GENERAL -> true
            ReminderTarget.GOAL -> goalNames.isNotEmpty() || random.nextFloat() < 0.35f
            ReminderTarget.DEBT -> debtNames.isNotEmpty() || random.nextFloat() < 0.35f
          }
      }

    val chosen =
      (validCandidates.ifEmpty { tonePool.filter { it.index != lastIndex }.ifEmpty { tonePool } })
        .random(random)

    val goal = goalNames.takeIf { it.isNotEmpty() }?.random(random)
    val debt = debtNames.takeIf { it.isNotEmpty() }?.random(random)
    return RenderedReminder(
      index = chosen.index,
      tone = chosen.value.tone,
      title = chosen.value.title,
      body = chosen.value.bodyBuilder(goal, debt),
    )
  }
}
