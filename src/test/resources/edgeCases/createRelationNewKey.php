<?php

class Customer extends \Hyperf\Database\Model\Model {
}

class User extends \Hyperf\Database\Model\Model {
    public function customers() : \Hyperf\Database\Model\Relations\HasMany
    {
        return $this->hasMany(Customer::class);
    }
}

$user = new User();

$user->customers()->create([
    '<caret>',
]);
