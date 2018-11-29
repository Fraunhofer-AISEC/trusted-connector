We welcome contributions to this repository!

If you plan to contribute a feature, please note that the development roadmap of this project is agreed in the research project _Industrial Data Space_ and the _Industrial Data Spaces Association_. We recommend that you contact us by mail before starting on larger contributions to make sure they fit into the overall roadmap.

Bug fixes can of course be provided immediately as a pull request.


# Submitting changes

Please send a [GitHub Pull Request](https://github.com/industrial-data-space/trusted-connector/pull/new/develop) with a clear list of what you've done (read more about [pull requests](http://help.github.com/pull-requests/)). When you send a pull request, we will love you forever if you include RSpec examples. We can always use more test coverage. Please follow our coding conventions (below) and make sure all of your commits are atomic (one feature per commit).

Always write a clear log message for your commits. One-line messages are fine for small changes, but bigger changes should look like this:

    $ git commit -m "<Keyword>: A brief summary of the commit
    > 
    > A paragraph describing what changed and its impact."

Prepend your commit message with a keyword indicating the type of contribution:

* `Fix`: A bug fix
* `Feature(<component>)`: A new feature in a component. The `<component>` typically refers to one of the sub-projects. For example, if you extend the web console by a login screen, you would write `Feature(webconsole): Add login screen for user authorization`
* `Minor`: If you did not change the code semantics but reformatted something

# Coding Conventions

This project uses [Google Code](https://github.com/google/google-java-format) style. Please make sure that you have an appropriate formatter installed for your IDE if you want to contribute.
