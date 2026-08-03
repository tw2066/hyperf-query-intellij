<?php

namespace App {
class User extends \Hyperf\Database\Model\Model
{
    public function customer(): \Hyperf\Database\Model\Relations\Relation
    {
        return $this->hasOne(Customer::class);
    }
}

class Customer extends \Hyperf\Database\Model\Model
{
}
}

(new \App\User())->newQuery()->with(['customer' => function (\Hyperf\Database\Model\Relations\Relation $customer) {
    $customer->where('<caret>');
}]);