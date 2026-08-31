<?php

namespace App {
    class GoodsModel extends \Hyperf\Database\Model\Model
    {
        protected ?string $connection = 'goods';

        protected $table = 'jc_goods';
    }
}
