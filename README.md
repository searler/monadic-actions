# monadic-actions
Monadic composition of Futures with error handling

A generalization of [play-monadic-actions](https://github.com/Kanaka-io/play-monadic-actions),
removing the dependency on [play](https://www.playframework.com/). 
For the core project, this was easily achieved by replacing all references to play.api.mvc.Result with the generic type R.

The design principles of the originating project are well founded and are useful in any context
that manipulates Futures. For example, the generalized implementation can be used to simplify
the scenarios illustrated in [FutureWorkflow](https://github.com/searler/FutureWorkflow).

Generalizing the cats and scalaz projects is not straightforward since they all depend on a type constructor
(a monad) that is generalized on a single type. The generalized form now has two types.


