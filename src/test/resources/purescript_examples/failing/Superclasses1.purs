module Main where

class Su a where
  su :: a -> a

class (Su a) <= Cl a where
  cl :: a -> a -> a

instance clNumber :: Cl Number where
  cl n m = n + m

