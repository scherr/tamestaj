module Mini where

import Data.Maybe

data EnvValue = B Bool | I Int

toBool :: EnvValue -> Bool
toBool (B b) = b

toInt :: EnvValue -> Int
toInt (I i) = i

type Env = [(String, EnvValue)]

newtype IntE  = IntE  (Env -> Int)
newtype IntV  = IntV  String
newtype BoolE = BoolE (Env -> Bool)
newtype BoolV = BoolV String
newtype Stmt  = Stmt  (Env -> Env)

add :: IntE -> IntE -> IntE
add (IntE a) (IntE b) = IntE (\env -> (a env) + (b env))

mul :: IntE -> IntE -> IntE
mul (IntE a) (IntE b) = IntE (\env -> (a env) * (b env))

eq :: IntE -> IntE -> BoolE
eq (IntE a) (IntE b) = BoolE (\env -> (a env) == (b env))

leq :: IntE -> IntE -> BoolE
leq (IntE a) (IntE b) = BoolE (\env -> (a env) <= (b env))

_and :: BoolE -> BoolE -> BoolE
_and (BoolE a) (BoolE b) = BoolE (\env -> (a env) && (b env))

_or :: BoolE -> BoolE -> BoolE
_or (BoolE a) (BoolE b) = BoolE (\env -> (a env) || (b env))

neg :: IntE -> IntE
neg (IntE a) = IntE (\env -> -(a env))

_not :: BoolE -> BoolE
_not (BoolE a) = BoolE (\env -> not (a env))

intVar :: String -> IntV
intVar name = IntV name

boolVar :: String -> BoolV
boolVar name = BoolV name

intRead :: IntV -> IntE
intRead (IntV name) =
	IntE (\env -> toInt (fromJust (lookup name env)))

boolRead :: BoolV -> BoolE
boolRead (BoolV name) =
	BoolE (\env -> toBool (fromJust (lookup name env)))

intAssign :: IntV -> IntE -> Stmt
intAssign (IntV name) (IntE e) =
	Stmt (\env -> (name, I (e env)) : (filter (\t -> (fst t) /= name) env))

boolAssign :: BoolV -> BoolE -> Stmt
boolAssign (BoolV name) (BoolE e) =
	Stmt (\env -> (name, B (e env)) : (filter (\t -> (fst t) /= name) env))

intLit :: Int -> IntE
intLit i = IntE (\_ -> i)

boolLit :: Bool -> BoolE
boolLit b = BoolE (\_ -> b)

whileDo :: BoolE -> Stmt -> Stmt
whileDo (BoolE test) (Stmt s) =
	let loop env = if test env then loop (s env) else env
	in Stmt loop

_then :: Stmt -> Stmt -> Stmt
_then (Stmt sA) (Stmt sB) = Stmt (\env -> sB (sA env))

intRun :: Stmt -> IntV -> Int
intRun (Stmt s) (IntV name) = toInt (fromJust (lookup name (s [])))

boolRun :: Stmt -> BoolV -> Bool
boolRun (Stmt s) (BoolV name) = toBool (fromJust (lookup name (s [])))


factorial :: Int -> Int
factorial x =
	let a = intVar("a")
	    n = intVar("n")
    in (intAssign a (intLit 1)) `_then`
       (intAssign n (intLit x)) `_then`
       (whileDo (leq (intLit 1) (intRead n)) (
       	  (intAssign a (mul (intRead a) (intRead n))) `_then`
          (intAssign n (add (intRead n) (intLit (-1))))
       )) `intRun` a

main :: IO ()
main = print (factorial 3)