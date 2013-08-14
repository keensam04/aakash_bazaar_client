Aakash Bazaar
=============

About
-----

Aakash Bazaar is an Android application for Aakash tablet specially
meant for browsing application developed for Aakash. This is a client
end which searches for updates or new applications on a server. The
server is running F-droid server which actually hosts all the apks.

.. image::
   https://raw.github.com/androportal/aakash_bazaar_client/master/res/drawable/screenshots/ab-menu-list.png

On client end we have a repository hierarchy. When started, it search
for a local server, if a local server exist, it fetches application
details like name, summary, description, icon etc and displays it in a
form of list. A user can click on an application to see detail
description like screenshots and ratings(yet to implement!!).

.. image::
   https://raw.github.com/androportal/aakash_bazaar_client/master/res/drawable/screenshots/ab-app-list.png

To set up a server please follow this `link
<https://github.com/androportal/f-droid-fdroidserver/blob/master/README.rst>`_. Once
the server is setup and working, you can add the server address to
Aakash Bazaar by going to 'Menu' -> 'Manage Repos'. To update the
application list with newly added repository, please click 'Update
Repos' under 'Menu'.

.. image::
   https://raw.github.com/androportal/aakash_bazaar_client/master/res/drawable/screenshots/ab-manage-repo.png

This repo contains the modified version of `F-Droid
<http://gitorious.org/f-droid/fdroidclient>`_ application.

You may download the latest version of aakash bazaar from `here
<http://www.it.iitb.ac.in/AakashApps/repo/aakash-bazaar.apk>`_


Contribute
----------

#. Clone this repo by typing ::

    git clone https://github.com/androportal/aakash_bazaar_client.git

#. Make a separate branch from master ::

    git branch new-feature

#. Checkout to new branch ::
	 
    git checkout new-feature

#. Send us a pull request


Warning
-------

Applications such as APL(Aakash programming lab) and ABT(Aakash
business tool) are not compatible with other android devices as they
have system level dependencies.


License
-------

Distributed under `GNU GPL Version 3
<http://www.gnu.org/licenses/gpl-3.0.txt>`_

All rights belong to the National Mission on Education through ICT,
MHRD, Government of India.


