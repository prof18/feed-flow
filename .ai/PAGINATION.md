# Timeline pagination

Why `selectFeeds` (`FeedItem.sq`) and the cursor handling in `FeedStateRepository`
look the way they do.

## Keyset cursor, never LIMIT/OFFSET

`OFFSET` addresses rows by position: "skip the first N of whatever this query
returns right now". The timeline filters `is_read = 0` while mark-as-read-on-scroll
flips rows to read *as the user scrolls*, so the result set shrinks underneath and
position N stops meaning the same row — each page then skips about a page worth of
unread articles. That was issue #1319.

A cursor addresses rows by identity instead ("the rows sorting strictly after *this*
row"), so it survives the set changing.

## The cursor is a pair

`pub_date` is not unique, so it can't identify a row alone. The `ORDER BY` breaks ties
on `url_hash`, and the cursor has to be that same `(pub_date, url_hash)` pair or paging
lands mid-way through same-timestamped articles and repeats or skips them.

## Why four branches

`pub_date` is nullable, and two things collide: SQLite sorts NULLs **first with ASC,
last with DESC**, and comparing a NULL date yields NULL rather than false — which
`WHERE` discards just like false. A plain comparison would therefore drop every undated
article permanently. Hence one branch per sort order, times whether the cursor itself
is dated:

| Sort | Cursor | Rows after it |
|---|---|---|
| DESC | dated | older dated rows, **plus all undated** (they're at the tail) |
| DESC | undated | only undated rows further along the tiebreak |
| ASC | dated | only later dated rows (undated already passed at the head) |
| ASC | undated | remaining undated rows, **plus all dated** |

## Easy to break

- The first-page gate tests `:lastUrlHash IS NULL`, not `:lastPubDate` — a real cursor
  can legitimately hold a NULL date, so only the `NOT NULL` hash can mean "no cursor".
- Invalidate the cursor only *after* a query succeeds: a failed refresh leaves the list
  on screen, so a nulled cursor re-appends page one as duplicates.
- End of list is "last page shorter than page size", not `size % pageSize` —
  hide-read-items removes rows from the list, so its size proves nothing.
- The predicate must mirror the `ORDER BY` exactly. Change one, change the other.
