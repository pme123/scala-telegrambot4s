# Telegram Bot Demo with Scala

This page was originally created for a [JUGs workshop](https://www.jug.ch/html/events/2017/chatbot_programmieren.html).

The workshop is about the first steps on implementing a chat bot with:

* [Telegram](https://telegram.org) a Messaging app.
* [telegrambot4s](https://github.com/mukel/telegrambot4s) a Scala API for Telegram.

The following chapters document the workshop step by step.

**A basic understanding of SBT and Scala will help;).**

# Preparation for the Workshop
## Create an account for Telegram
To get an account for Telegram you need a mobile number.

So the easiest way is to install the mobile app in your app store:

![image](https://user-images.githubusercontent.com/3437927/35958215-dfd0646e-0c9f-11e8-9fd4-6487a4822db3.png)

## Setup the Scala project
To make sure that every one is on the same page, we start with the same Project.

* Make sure SBT is installed - you need sbt >= 1.0. Here the steps: https://www.scala-sbt.org/1.0/docs/Setup.html.
* now on the terminal wherever you want the project to be: $`sbt new scala/scala-seed.g8`
* If everything compiles and the test runs on your IDE, you are done!
# You are ready to go!

# Workshop
This part we do together step by step.
## What
We want to write a Bot that does something for us:
* Simplify or enabling a Workflow (that's my personal Use Case)
* Bring a FAQ to the users
* Provide information without the need to provide a GUI!
* Or just for fun!
## How
![image](https://user-images.githubusercontent.com/3437927/35958755-c1bbd08c-0ca2-11e8-93aa-13e8f18746b0.png)

Each Messaging Provider (e.g. Telegram) provides an **API** to interact with the Messaging infrastructur.
That's what your Bot needs to communicate with the world. 
These APIs are usually stateless web services that return data structure as JSON.
As this is messaging **everything is asynchronous** (I get back to that)!

Now to simplify that, you find **Wrappers** in some programming languages. 
As for Scala they are a bit rare - so thankfully for Telegram there is a great one:
[telegrambot4s](https://github.com/mukel/telegrambot4s) 

And on top of that comes **your Bot**!

## Setup the Bot
* Register a user on [Telegram](https://telegram.org) if you don't have one.
* Search for the BotFather in the Telegram App (he helps you to setup and manage your bots).
* Ask the BotFather for a new bot: `/newbot`
* He will guide you through the simple process (nice example of a chat bot;).
* The generated token is all what you need!

## Setup the Project
* Go to the project you setup before the workshop.
* Add the dependency to our Wrapper: `libraryDependencies += "info.mukel" %% "telegrambot4s" % "3.0.14"`

First we want to **provide the token** in a safe way to avoid leaking it.
* Create a package object in the `example` package and add:
```
lazy val botToken: String = scala.util.Properties
  .envOrNone("BOT_TOKEN")
  .getOrElse(Source.fromResource("bot.token").getLines().mkString)
```
* You can now add a `bot.token` with your token and put it in your `src/main/resources` folder.
Add `bot.token` to your `.gitignore` if you have a public repo (otherwise you get a mail from the Github's Security Agent;)
* (or if security is of no concern, you can add it directly: `lazy val botToken = "[BOT_TOKEN]"`

## Webhooks vs Polling
Before we start let's explain shortly the difference between Webhooks and Polling. This is the quote from the [Scala API](https://github.com/mukel/telegrambot4s):
> Both methods are fully supported. Polling is the easiest method; it can be used locally without any additional requirements. It has been radically improved, doesn't flood the server (like other libraries do) and it's pretty fast.

> Using webhooks requires a server (it won't work on your laptop). For a comprehensive reference check Marvin's Patent Pending Guide to All Things Webhook.

So for this workshop, or examples in general **Polling** is the way to go.
## Hello User Bot
Let's greet the Bot and it should return that with a personalized greeting.

* Create a Scala object in the bots package: `HelloBot`
```
object HelloBot
  extends TelegramBot // the general bot behavior
    with Polling // we use Polling
    with Commands { // and we want to listen to Commands

  lazy val token: String = botToken // the token is required by the Bot behavior

  onCommand('hello) { implicit msg => // listen for the command hello and
    reply(s"Hello ${msg.from.map(_.firstName).getOrElse("")}!") // and reply with the personalized greeting
  }
}
```
* As we don't no infrastructure all we need is to run our Bot:
```
object BotApp extends App {
  HelloBot.run()
}
```
* Next we need our friend `BotFather` to create our command: `/setcommands`
* The easiest way is to manage your commands in file, from where you can take them (you always overwrite them for a bot). e.g. `botcommands.txt`:
```
hello - Simple Hello World.
```
* Like before the `BotFather` will help with this.
* Now lets say hello to your Bot (the command should be available, when hitting `/` in the text-field).

## Counter Bot
(Here the [Solution Branch](https://github.com/pme123/play-scala-telegrambot4s/tree/add-callback-bot) if you have problems)
### Callbacks
The first step to implement a conversation with a user is to understand the concept of `callbacks`.
To guide the user through a conversation you can provide a [keyboard](https://core.telegram.org/bots#keyboards).
These keys (buttons) are identified with a callback identifier.

Create a Scala class `CounterBot` in the `bots` package (you can copy the `HelloBot`:

* We will listen for the Command `/counter` to start the process:
```
  onCommand("/counter") { implicit msg =>
    reply("Press to increment!", replyMarkup = Some(markupCounter(0)))
  }
```
* The logic for the counting and the creation of the button:
```
  private def markupCounter(n: Int): InlineKeyboardMarkup = {
    requestCount += 1
    InlineKeyboardMarkup.singleButton( // set a layout for the Button
      InlineKeyboardButton.callbackData( // create the button into the layout
        s"Press me!!!\n$n - $requestCount", // text to show on the button (count of the times hitting the button and total request count)
        tag(n.toString))) // create a callback identifier
  }
```
* The callback identifier is composed by a static tag and the actual count:
```
  private val TAG = "COUNTER_TAG"
  private def tag: String => String = prefixTag(TAG)
```
* When the user hits the button we can listen for it:
```
  onCallbackWithTag(TAG) { implicit cbq => // listens on all callbacks that START with TAG
    // Notification only shown to the user who pressed the button.
    ackCallback(Some(cbq.from.firstName + " pressed the button!"))
    // Or just ackCallback() - this is needed by Telegram!

    for {
      data <- cbq.data //the data is the callback identifier without the TAG (the count in our case)
      Extractors.Int(n) = data // extract the optional String to an Int
      msg <- cbq.message
    } /* do */ {
      request(
        EditMessageReplyMarkup( // to update the existing button - (not creating a new button)
          Some(ChatId(msg.source)), // msg.chat.id
          Some(msg.messageId),
          replyMarkup = Some(markupCounter(n + 1))))
    }
  }
```
* Like before we need a runner app:
```
object CounterBotApp extends App {
  CounterBot.run()
}
```
* we want to reuse our bot, so we only overwrite the command to `counter - Counts the time a User hits the button.`like above.
  As the commands are set always in one step it makes sense to manage them in file. Create 'bot-commands.txt` file and add:
```
  hello - Simple Hello World.
  counter - Counts the time a User hits the button.
```
* Run the `CounterBotApp` and select the command `/counter`
* Hit the button and create new buttons width `/counter`

# Next steps (Next workshop;)
This was the basic workshop. 
## JSON-Client
As mentioned above the wrapper hides the actual API. 
If you want to check that out, check out the first version of this Workshop:
* [play-scala-telegrambot4s](https://github.com/pme123/play-scala-telegrambot4s)
* Here the reference: [JSON-API from Telegram](https://core.telegram.org/api)

## Handling conversations
Now we want to do complexer conversations. To get to this next level we need quite some ingredients:

* Handle the state of each user.
* Create a FSM (finite state machine) to design the conversation.
* A running App that easily integrates everything - and in a later state provides the webhooks.

I created a small library for that, check out: [play-akka-telegrambot4s](https://github.com/pme123/play-akka-telegrambot4s)