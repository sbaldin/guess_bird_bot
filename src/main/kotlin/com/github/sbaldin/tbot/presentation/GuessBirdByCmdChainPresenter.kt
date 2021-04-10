package com.github.sbaldin.tbot.presentation

import com.elbekD.bot.Bot
import com.elbekD.bot.feature.chain.ChainBuilder
import com.elbekD.bot.feature.chain.chain
import com.elbekD.bot.types.KeyboardButton
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.ReplyKeyboardMarkup
import com.github.sbaldin.tbot.data.BotConf
import com.github.sbaldin.tbot.domain.BiggestPhotoInteractor
import com.github.sbaldin.tbot.domain.BirdClassifierInteractor
import com.github.sbaldin.tbot.hasPhoto
import com.github.sbaldin.tbot.presentation.base.BaseGuessBirdChainPresenter
import com.github.sbaldin.tbot.toPercentage
import java.text.MessageFormat

class GuessBirdByCmdChainPresenter(
    conf: BotConf,
    photoInteractor: BiggestPhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder = bot.chain("guess_bird_start", { msg -> msg.text in startChainPredicates }) { msg ->
        bot.sendMessage(msg.chat.id, startDialogMsg)
    }.then(label = "guess_bird_photo_guess_step") { msg ->
        if (msg.new_chat_photo == null && msg.photo == null) {
            abortChain(bot, msg.chat.id, abortDialogMsg)
            return@then
        }
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)

        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
        birdClassDistributionByChatId[msg.chat.id] = birdDistribution

        bot.sendMessage(
            chatId = msg.chat.id,
            text = MessageFormat.format(
                hypothesisMsg,
                getBirdName(bestBird),
                bestBird.rate.toPercentage()
            ),
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(guessingSuccessKeyboard),
                        KeyboardButton(guessingFailKeyboard)
                    )
                )
            )
        )
    }.then(label = "guess_bird_photo_finish_step") { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }
}

class GuessBirdByChatPhotoChainPresenter(
    conf: BotConf,
    photoInteractor: BiggestPhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    override fun chain(bot: Bot): ChainBuilder = bot.chain("new_photo_in_chat_start", { m -> m.hasPhoto() }) { msg ->
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
        birdClassDistributionByChatId[msg.chat.id] = birdDistribution

        bot.sendMessage(
            chatId = msg.chat.id,
            text = MessageFormat.format(
                hypothesisMsg,
                getBirdName(bestBird),
                bestBird.rate.toPercentage()
            ),
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(guessingSuccessKeyboard),
                        KeyboardButton(guessingFailKeyboard)
                    )
                )
            )
        )
    }.then(label = "guess_bird_photo_finish_step") { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }
}

class GuessBirdByChatMentionChainPresenter(
    conf: BotConf,
    photoInteractor: BiggestPhotoInteractor,
    birdInteractor: BirdClassifierInteractor
) : BaseGuessBirdChainPresenter(conf.locale(), conf.token, photoInteractor, birdInteractor) {

    private val botName = conf.name

    private fun chainPredicateFn(msg: Message) =
        msg.text?.contains(botName) ?: false && startChainPredicates.any { msg.text?.contains(it) ?: false }

    override fun chain(bot: Bot): ChainBuilder = bot.chain("mention_in_chat", this::chainPredicateFn) { msg ->
        bot.sendMessage(msg.chat.id, guessingInProgressMsg)
        val birdDistribution = getBirdClassDistribution(bot, msg)
        val bestBird = birdInteractor.getBirdWithHighestRate(birdDistribution)
        birdClassDistributionByChatId[msg.chat.id] = birdDistribution

        bot.sendMessage(
            chatId = msg.chat.id,
            text = MessageFormat.format(
                hypothesisMsg,
                getBirdName(bestBird),
                bestBird.rate.toPercentage()
            ),
            markup = ReplyKeyboardMarkup(
                resize_keyboard = true,
                one_time_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(guessingSuccessKeyboard),
                        KeyboardButton(guessingFailKeyboard)
                    )
                )
            )
        )
    }.then(label = "guess_bird_photo_finish_step") { msg ->
        var lastDialogMsg = ""
        when (msg.text) {
            guessingSuccessKeyboard -> {
                lastDialogMsg = finishSuccessDialogMsg
            }
            guessingFailKeyboard -> {
                val distribution = birdClassDistributionByChatId.getValue(msg.chat.id)
                val fiveBirdsMsg = birdInteractor.getBirdsWithHighestRate(distribution).mapIndexed { index, bird ->
                    birdWithEmojiNumber(index, bird)
                }.joinToString("")
                lastDialogMsg = finishFailDialogMsg + fiveBirdsMsg
            }
        }
        abortChain(bot, msg.chat.id, lastDialogMsg)
    }
}