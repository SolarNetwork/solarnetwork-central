# SolarNetwork HTTP Message Signatures (RFC 9421)

SolarNetwork accepts [RFC 9421][rfc9421] HTTP Message Signatures on the SolarQuery and
SolarUser APIs, alongside the [SNWS2][snws2] scheme. Both use the same security token
credentials: a token ID and a token secret. Nothing needs to be registered to use RFC 9421
with a token that already works with SNWS2.

This document specifies the SolarNetwork profile of RFC 9421 — the parts the standard
leaves to the application.

## Presenting a signature

RFC 9421 is not an HTTP authentication scheme, so there is no `Authorization` header to
change. A signature is carried in two fields:

```
Signature-Input: sn=("@method" "@authority" "@path" "@query");created=1789761471\
;keyid="MY-TOKEN-ID";tag="solarnetwork"
Signature: sn=:yt00kJV0rsRBmP7Pu7C8J2f+avzPAAzv1vEGX+SrCd8=:
```

SolarNetwork detects an RFC 9421 request by the presence of `Signature-Input`, as
[RFC 9421 appendix A][detect] recommends. If a request carries an `Authorization` header
naming a supported scheme, that takes precedence, since it is an unambiguous statement of
how the client means to authenticate.

The signature label (`sn` above) can be anything.

## Signature parameters

| Parameter | Required | Rule |
|:----------|:---------|:-----|
| `keyid`   | yes | The token ID, optionally with a signing key date; see [Signing keys](#signing-keys) |
| `created` | yes | Must be within the server's allowed date skew, which is 15 minutes |
| `expires` | no  | If given, must not be in the past |
| `alg`     | no  | If given, must be `hmac-sha256` |
| `nonce`   | no  | Accepted and ignored |
| `tag`     | no  | See [Choosing a signature](#choosing-a-signature) |

## Covered components

RFC 9421 lets a signer cover as much or as little of a request as it likes. SolarNetwork
requires a minimum set, so that a signature commits to the whole request the way an SNWS2
signature does:

* `@method`
* `@authority` and `@path` — or `@target-uri`, which covers both
* `@query` — required when the request has a query string
* `content-digest` — required when the request has content; see [Request content](#request-content)
* `content-type` — required when the request includes one
* every `X-SN-*` header the request includes

Any additional component may be covered. The `sf`, `key`, `bs` and `name` component
parameters are supported.

These are rejected:

* the `tr` parameter — trailer fields are not supported
* the `req` parameter — not valid on a request signature ([RFC 9421 §2.4][req])
* `@status` — applies to responses
* the same component identifier covered more than once

## Signing keys

The `keyid` parameter selects both the token and how its secret becomes the signing key.
Both forms are accepted; the derived key is the one SolarNetwork's own tooling uses by
default, and the one to prefer.

### `keyid="<token ID>:<YYYYMMDD>"` — derived key, recommended

The signing key is derived from the token secret and a UTC date:

```
keyid="MY-TOKEN-ID:20260918"
  →   K = HMAC(HMAC("SNWS3" + tokenSecret, "20260918"), "snws3_request")
```

This works the way an [SNWS2 signing key][snws2key] does, and has the same benefit: the
derived key is only valid for 7 days from its date, so it can be given to whatever signs
the request without disclosing the token secret itself. The date must not be more than 6
days before, or 1 day after, the UTC date of the `created` parameter.

### `keyid="<token ID>"` — token secret

The signing key is the UTF-8 encoded token secret:

```
keyid="MY-TOKEN-ID"   →   K = UTF8(tokenSecret)
```

Any RFC 9421 implementation can sign this way, with nothing but the token ID and secret
and no knowledge of SolarNetwork's key derivation. That makes it the simplest way to get
started, at the cost of handing the secret to the signer.

## Request content

RFC 9421 has no notion of message content: a request body is covered by covering a field
that digests it. SolarNetwork uses [RFC 9530][rfc9530] `Content-Digest` for this, with
either `sha-256` or `sha-512`:

```
Content-Digest: sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:
```

Note this is a different field from the RFC 3230 `Digest` field the SNWS2 scheme accepts.
A `Content-Digest` on the request is validated against the actual content whether or not
it is covered by the signature, and a request with content must cover it.

This includes `application/x-www-form-urlencoded` content. The SNWS2 scheme signs form
parameters as if they were query parameters; RFC 9421 does not, so form content needs a
`Content-Digest` like any other content.

## Choosing a signature

A request may carry more than one signature, for example when a proxy adds its own. To say
which one SolarNetwork should authenticate with, give it the `tag` parameter:

```
;tag="solarnetwork"
```

If no signature carries the tag and exactly one signature is present, that one is used. If
no signature carries the tag and several are present, the request is rejected: SolarNetwork
will not guess.

## Rejected requests

A rejected request gets a `401` with the reason in the `X-SN-ErrorMessage` header, and an
[`Accept-Signature`][acceptsig] field describing what an acceptable signature must cover:

```
Accept-Signature: sn=("@method" "@authority" "@path" "@query");created;keyid\
;tag="solarnetwork"
```

## Browser clients

None of `Signature-Input`, `Signature` or `Content-Digest` is a
[CORS-safelisted request header][safelist], so a cross-origin browser request carrying a
signature is preflighted. The SolarQuery and SolarUser CORS configuration allows all
three, alongside the `Authorization`, `Content-MD5`, `Content-Type`, `Digest` and
`X-SN-Date` headers the other schemes use.

Two response headers are exposed, so a browser client can read them:
`Accept-Signature`, and the `X-SN-ErrorMessage` header carrying the rejection reason.

## Worked example

For this request:

```
GET /solarquery/api/v1/sec/datum/stream/datum?nodeId=123&sourceId=meter HTTP/1.1
Host: localhost:9082
```

signed with token `12345678901234567890`, secret `lsdjfpse9jfoeijfe09j`, at
`created=1789761471` (2026-09-18 UTC), the signature base is:

```
"@method": GET
"@authority": localhost:9082
"@path": /solarquery/api/v1/sec/datum/stream/datum
"@query": ?nodeId=123&sourceId=meter
"@signature-params": ("@method" "@authority" "@path" "@query");created=1789761471\
;keyid="12345678901234567890:20260918";tag="solarnetwork"
```

and `HMAC-SHA256` of that, keyed with the derived signing key
`HMAC(HMAC("SNWS3" + tokenSecret, "20260918"), "snws3_request")`, gives:

```
Signature-Input: sn=("@method" "@authority" "@path" "@query");created=1789761471\
;keyid="12345678901234567890:20260918";tag="solarnetwork"
Signature: sn=:8gcZ/Rdd932qOF1Cm+hpu7M/hQQ92A3dCffFxtyTSWg=:
```

Signing the same request with the token secret directly — `keyid` of just
`12345678901234567890` — gives
`Signature: sn=:yt00kJV0rsRBmP7Pu7C8J2f+avzPAAzv1vEGX+SrCd8=:` instead.

The [API Explorer][explorer] shows these steps for any request you give it, and generates a
matching `curl` command.

## Configuration

Each app exposes the profile under `app.web.security.token.http-signatures`:

| Property | Default | Description |
|:---------|:--------|:------------|
| `enabled` | `true` | Whether RFC 9421 signatures are accepted at all |
| `tag` | `solarnetwork` | The tag that identifies the signature to authenticate with |
| `require-tag` | `false` | Whether a signature must carry the tag |
| `algorithms` | `hmac-sha256` | The acceptable signature algorithms |
| `allow-direct-secret-key` | `true` | Whether `keyid` may be a bare token ID |
| `allow-derived-signing-key` | `true` | Whether `keyid` may carry a signing date |
| `derived-signing-key-max-days` | `7` | How long a derived signing key remains valid |

SolarJobs sets `enabled: false`, as it exposes an internal management API only.

[acceptsig]: https://www.rfc-editor.org/rfc/rfc9421.html#name-the-accept-signature-field
[detect]: https://www.rfc-editor.org/rfc/rfc9421.html#name-detecting-http-message-sign
[explorer]: https://go.solarnetwork.net/dev/api/
[req]: https://www.rfc-editor.org/rfc/rfc9421.html#name-signing-request-components-
[rfc9421]: https://www.rfc-editor.org/rfc/rfc9421.html
[rfc9530]: https://www.rfc-editor.org/rfc/rfc9530.html
[safelist]: https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_request_header
[snws2]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2
[snws2key]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2#signing-key
