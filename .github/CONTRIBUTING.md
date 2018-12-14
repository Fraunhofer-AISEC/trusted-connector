# Welcome, Contributor!

If you plan to contribute a feature, please note that the development roadmap of this project is agreed in the research project _Industrial Data Space_ and the _Industrial Data Spaces Association_. We recommend that you contact us by mail before starting on larger contributions to make sure they fit into the overall roadmap.

Bug fixes can of course be provided immediately as a pull request.

## Submitting changes

Please send a [GitHub Pull Request](https://github.com/industrial-data-space/trusted-connector/pull/new/develop) with a clear list of what you've done (read more about [pull requests](http://help.github.com/pull-requests/)). Please follow our coding conventions (below) and make sure all of your commits are atomic (one feature per commit). When adding new features, please add tests demonstrating the expected fuctionality.

Always write a clear log message for your commits. One-line messages are fine for small changes, but bigger changes should look like this:

    $ git commit -m "<Keyword>: A brief summary of the commit
    > 
    > A paragraph describing what changed and its impact."

Prepend your commit message with a keyword indicating the type of contribution:

* `Fix`: A bug fix
* `Feature(<component>)`: A new feature in a component. The `<component>` typically refers to one of the sub-projects. For example, if you extend the web console by a login screen, you would write `Feature(webconsole): Add login screen for user authorization`
* `Minor`: If you did not change the code semantics but reformatted something

## Coding Conventions

This project uses [Google Code](https://github.com/google/google-java-format) style. Please make sure that you have an appropriate formatter installed for your IDE if you want to contribute.


## Legal

Contributions to this project are always under the same license as the main project. This is standard practice to keep everything on safe legal ground and is also included in the [Github Terms of Service](https://help.github.com/articles/github-terms-of-service/#6-contributions-under-repository-license).

To make sure that contributions comply with the license we need you to sign off all commits in your pull requests with the line 
```
Signed-off-by: <Your real name> <your@company.mail.com>
```
which can be automatically generated with the `--signoff` switch:
```
git commit --signoff "<commit message>"
```

If your commits do not include this line, your pull request may not be accepted and you will be asked to add the sign-off message.
