Bodylog
=======

Bodylog is a combination Android app and server that allows _you_ to collect
data on _you_. It's essentially a hosted version of Google Latitude; the
differences are that, since you own your data, there are no privacy concerns,
and since the data is written out in CSV as it's gathered, you can perform
complex queries on your location.

Using Bodylog
-------------

First, get the server up and running. I'll write how to do that once I've
actually written the server. The server has one handler: POST requests to /ping
(used for updating location). The full URL (with ping on the end) is your
endpoint; if I run my server on example.com, my endpoint is
http://example.com:8010/ping.

After you install the Android app, you'll see that that app shows you a "body
identifier". This is a randomly-generated tag that is sent along with all of
your requests to your Bodylog server. You should configure your server to accept
only this body identifier (or multiple, if you want to share the server with
others).  The server will ignore requests that do not have a whitelisted body
identifier sent as a POST variable.

You can configure where the server stores your data; by default, it stores it at
/tmp/bodylog.db. Output is a sqlite3 database with one table, `location`, with
columns `time, device, latitude, longitude, altitude`.

Note that altitude is going to be incredibly unreliable, because Bodylog uses
network location to save on battery usage.
