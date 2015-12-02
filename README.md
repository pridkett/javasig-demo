Watson Demo Application for New York Java SIG
=============================================

Copyright (c) 2015 IBM Corporation

Patrick Wagstrom <pwagstro@us.ibm.com>

***IMPORTANT: While Making This Demo Application I Discovered a Few Defects in
 the [Watson Developer Cloud Java SDK][wdc-sdk]. This required the use of a
 patched library, which is not yet publicly available. This will be remedied
 soon.***

This is a simple little demo application that I created to show off a handful
of Watson Developer Cloud services and how you can stitch them together to
make intelligent cognitive applications. In particular, this application uses:

* [Speech to Text][stt]
* [Text to Speech][tts]
* [Language Translation][lt]
* [Natural Language Classifier][nlc]

Installation and Setup
----------------------

Due to some interesting challenges that I had with getting audio to play back
using the standard Java APIs, this application only runs on Macs and Linux and
expects there to be a command `/usr/local/bin/play` that can accept a standard
wav file when sent over stdin.

Beyond that, you'll need to go to [IBM Bluemix][bluemix] and create an instance
of the following services:

* [Speech to Text][bm-stt]
* [Text to Speech][bm-tts]
* [Language Translation][bm-lt]
* [Natural Language Classifier][nlc-lt]

You'll then need to copy the service credentials for each provisioned service
into the appropriate spots in `configuration.properties`.

Finally, you'll need to train an instance of the Natural Language Classifier
Service using the data contained in `data/training.csv`. This is a little bit
beyond the scope of this document, but it involves some fun with REST calls
or the use of a few pieces of experimental tooling.

Once you have all those setup you can compile the application using the following
commands: `mvn clean compile package`. Right now there are no tests. This is really
a bit of a hack to show how things can work.

Usage
-----

To run the app just type: `./javasig-demo.sh`. By default it's expecting English
language input. Just type in your text it will be classified by Watson.

Here's a few more helpful commands:

* `\d`: toggles debugging, which gives you insight into data returned from Watson
* `\il LANGUAGE`: selects the language for input. You can list the available languages with `\il ?`. Not all languages currently support speech to text.
* `\ov VOICE`: selects the output voice for output. You list the available
voices with `\ov ?`. While it is possible to select a voice for a language other
than English, this usually results in strange pronunciations.
* `\q`: quits the application

Finally, if you enter a blank line the application will start recording audio. If you've set the input language to a language with speech recognition, it will automatically translate the recording into English to run it through the Natural
Language Classifier.

License
-------

Licensed under the terms of the Apache 2 License

[wdc-sdk]: https://github.com/watson-developer-cloud/java-sdk
[stt]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/speech-to-text.html
[tts]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/text-to-speech.html
[lt]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/language-translation.html
[nlc]: http://www.ibm.com/smarterplanet/us/en/ibmwatson/developercloud/nl-classifier.html
[bm-stt]: https://console.ng.bluemix.net/catalog/services/speech-to-text/
[bm-tts]: https://console.ng.bluemix.net/catalog/services/text-to-speech/
[bm-lt]: https://console.ng.bluemix.net/catalog/services/language-translation/
[nlc-lt]: https://console.ng.bluemix.net/catalog/services/natural-language-classifier/
[bluemix]: https://www.bluemix.net/