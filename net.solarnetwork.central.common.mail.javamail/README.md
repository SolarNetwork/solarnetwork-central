# Mail integration via JavaMail

This plugin provides a `org.springframework.mail.javamail.JavaMailSender` services that uses the
JavaMail API for managing the mail transport.


# Configuration

See the examples in `example/configuration`.

## net.solarnetwork.central.mail.javamail

These settings control aspects of the mail connection.

| Setting                | Default | Description |
|------------------------|---------|-------------|
| `host`                 | `localhost` | The SMTP port to use. |
| `port`                 | `25` | The SMTP port to use. |
| `username`             | | The SMTP username to use. |
| `password`             | | The SMTP password to use. |
| `javaMailProperties[]` | | A map of additional properties to configure on the Java Mail [SMTP Session][1]. For example `javaMailProperties[mail.smtp.starttls.enable] = true`. |

[1]: https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
