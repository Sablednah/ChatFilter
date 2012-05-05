Chat Filter
===========

A Basic profanity filter - and message when people mention trigger keywords.

### Config

kick: [true|false] Set to true to kick players who swear.

showInConsole: [true|false] Show player's name and the offending message in console.

censor: [true|false] Censor messages instead of blocking entire message.

agressiveMatching: [true|false]  Attempts to match more words by looking for 3=e 0=o etc.

Edit lang.yml (created on first run) to change the trigger words and notification text. 

### Commands

    /charfilter reload
Reload config and lang files.

### Permissions

    chatfilter.canswear
Users with this node are not censored.

    chatfilter.reload
Users with this node can reload config.

    chatfilter.blockchat
Users with this node cannot chat.

### Changelog
1.7.2:  Changed permissions node for chat to chatfilter.canchat

1.7.1:  Fixed config typo

1.7.0:  Aggressive matching option.  Permissions node to block chat completely. 

1.6.0:  Permissions node for reload.  Colour support.

1.5.0:  Permissions node to allow select users to swear.

1.4:  Optional censorship mode.  Replaced words with configurable text.

1.3:  Config options to match whole words.

1.2:  Option to show offending messages in console.

1.1:  Added kick option.

1.0:  First release.

